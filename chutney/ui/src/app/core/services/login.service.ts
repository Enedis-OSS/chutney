/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlSegment } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';

import { environment } from '@env/environment';
import { Authorization, User } from '@model';
import { contains, intersection, isNullOrBlankString } from '@shared/tools';
import { SsoService } from '@core/services/sso.service';
import { TranslateService } from '@ngx-translate/core';
import { AlertService } from '@shared';

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    private url = '/api/v1/user';
    private loginUrl = this.url + '/login';
    private NO_USER = new User('');
    private user$: BehaviorSubject<User> = new BehaviorSubject(this.NO_USER);
    private unauthorizedMessage: string
    private sessionExpiredMessage: string

    constructor(
        private http: HttpClient,
        private router: Router,
        private ssoService: SsoService,
        private translateService: TranslateService,
        private alertService: AlertService
    ) {
        this.unauthorizedMessage = this.translateService.instant('login.unauthorized')
        this.sessionExpiredMessage = this.translateService.instant('login.expired')
    }

    isAuthorized(requestURL: string, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        const token = this.getToken();
        if (token) {
            return of(this.isAuthorizedJwt(token, requestURL, route))
        } else if (this.ssoService.accessTokenValid) {
            return this.isAuthorizedSso(requestURL, route, state)
        } else {
            this.setUser(this.NO_USER)
            this.initLogin(requestURL);
            return of(false);
        }
    }

    private isAuthorizedJwt(token: string, requestURL: string, route: ActivatedRouteSnapshot) {
        const payload = this.decodeToken(token);
        if (payload) {
            const {sub, iat, exp, ...user} = payload
            if ((user == this.NO_USER || this.isTokenExpired(token)) && !this.ssoService.accessToken) {
                localStorage.removeItem('jwt')
                this.alertService.error(this.sessionExpiredMessage, {
                    timeOut: 0,
                    extendedTimeOut: 0,
                    closeButton: true
                });
                this.initLogin(requestURL)
                return false
            }
            this.user$.next(user as User)
            const authorizations: Array<Authorization> = route.data['authorizations'] || [];
            if (this.hasAuthorization(authorizations, this.user$.getValue())) {
                return true;
            }
            this.alertService.error(this.unauthorizedMessage, {timeOut: 0, extendedTimeOut: 0, closeButton: true});
            this.navigateAfterLogin();
        }
        return false
    }

    private isAuthorizedSso(requestURL: string, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.ssoService.canActivateProtectedRoutes$.pipe(
            switchMap(x => this.currentUser().pipe(
                tap(user => this.setUser(user)),
                map(user => {
                    const authorizations: Array<Authorization> = route.data['authorizations'] || [];
                    if (this.hasAuthorization(authorizations, this.user$.getValue())) {
                        this.navigateAfterLogin(requestURL);
                        return true;
                    } else {
                        this.alertService.error(this.unauthorizedMessage, {
                            timeOut: 0,
                            extendedTimeOut: 0,
                            closeButton: true
                        });
                        this.navigateAfterLogin();
                        return false;
                    }
                })
            )));
    }

    public getToken() {
        return localStorage.getItem('jwt');
    }

    initLogin(url?: string, headers?: HttpHeaders | {
        [header: string]: string | string[];
    }) {
        const nextUrl = this.nullifyLoginUrl(url);
        const queryParams: Object = isNullOrBlankString(nextUrl) ? {} : {queryParams: {url: nextUrl}};
        this.router.navigate(['login'], queryParams);
    }

    login(username: string, password: string): Observable<User> {
        if (isNullOrBlankString(username) && isNullOrBlankString(password)) {
            return this.currentUser().pipe(
                tap(user => this.setUser(user))
            );
        }

        return this.http.post<{ token: string }>(environment.backend + this.loginUrl, {username, password})
            .pipe(
                map(response => {
                    localStorage.setItem('jwt', response.token)
                    const {sub, iat, exp, ...user} = this.decodeToken(response.token);
                    this.setUser(user as User)
                    return user as User

                })
            );
    }

    navigateAfterLogin(url?: string) {
        const nextUrl = this.nullifyLoginUrl(url);
        if (this.isAuthenticated()) {
            const user: User = this.user$.getValue();
            this.router.navigateByUrl(nextUrl ? nextUrl : this.defaultForwardUrl(user));
        } else {
            this.router.navigateByUrl('/login');
        }
    }

    logout() {
        localStorage.removeItem('jwt')
        this.setUser(this.NO_USER)
        if (this.ssoService.accessToken) {
            this.ssoService.logout()
        }
        this.router.navigateByUrl('/login');

    }

    getUser(): Observable<User> {
        return this.user$;
    }

    isAuthenticated(): boolean {
        const user: User = this.user$.getValue();
        return this.NO_USER !== user;
    }

    hasAuthorization(authorization: Array<Authorization> | Authorization = [], u: User = null): boolean {
        const user: User = u || this.user$.getValue();
        const auth = [].concat(authorization);
        if (user != this.NO_USER) {
            return auth.length == 0 || intersection(user.authorizations, auth).length > 0;
        }
        return false;
    }

    currentUser(skipInterceptor: boolean = false, headers: HttpHeaders | {
        [header: string]: string | string[];
    } = {}): Observable<User> {
        const headersInterceptor = skipInterceptor ? {'no-intercept-error': ''} : {}
        const options = {
            headers: {...headersInterceptor, ...headers}
        };
        return this.http.get<User>(environment.backend + this.url, options);
    }

    private setUser(user: User) {
        this.user$.next(user);
    }

    private defaultForwardUrl(user: User): string {
        const authorizations = user.authorizations;
        if (authorizations) {
            if (contains(authorizations, Authorization.SCENARIO_READ)) return '/scenario';
            if (contains(authorizations, Authorization.CAMPAIGN_READ)) return '/campaign';
            if (contains(authorizations, Authorization.ENVIRONMENT_ACCESS)) return '/targets';
            if (contains(authorizations, Authorization.DATASET_READ)) return '/dataset';
            if (contains(authorizations, Authorization.ADMIN_ACCESS)) return '/';
        }

        return '/login';
    }

    private nullifyLoginUrl(url: string): string {
        return url && url !== '/login' ? url : null;
    }


    private decodeToken(token: string): JwtTokenPayload {
        if (!token) {
            return null;
        }
        const payload = token.split('.')[1];
        try {
            const user = JSON.parse(atob(payload));
            return user
        } catch (error) {
            console.error('Error while decoding token', error);
            return null;
        }
    }

    private isTokenExpired(token: string): boolean {
        const decodedToken = this.decodeToken(token);
        if (!decodedToken || !decodedToken.exp) {
            return true;
        }
        const expirationDate = new Date(0);
        expirationDate.setUTCSeconds(decodedToken.exp);
        return expirationDate < new Date();
    }
}

interface JwtTokenPayload {
    id: string,
    name: string,
    firstname: string,
    lastname: string,
    mail: string,
    authorizations: string[],
    sub: string,
    iat: number,
    exp: number,
}
