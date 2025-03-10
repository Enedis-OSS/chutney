/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from '@angular/common/http';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '@env/environment';
import { BehaviorSubject, combineLatest, firstValueFrom, map, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { JwtService } from '@core/services/jwt.service';


interface SsoAuthConfig {
    issuer: string,
    clientId: string,
    clientSecret: string,
    responseType: string,
    scope: string,
    redirectBaseUrl: string,
    ssoProviderName: string,
    ssoProviderImageUrl: string,
    headers: { [name: string]: string | string[]; },
    additionalQueryParams: { [name: string]: string | string[]; }
    oidc: boolean
}

@Injectable({
    providedIn: 'root'
})
export class SsoService {


    private isAuthenticatedSubject$ = new BehaviorSubject<boolean>(false);
    public isAuthenticated$ = this.isAuthenticatedSubject$.asObservable();

    private isDoneLoadingSubject$ = new BehaviorSubject<boolean>(false);
    public isDoneLoading$ = this.isDoneLoadingSubject$.asObservable();

    private resourceUrl = '/api/v1/sso/config';
    private ssoConfig: SsoAuthConfig
    private enableSso = false

    private readonly clockSkew = 60; // 60 seconds skew tolerance


    constructor(
        private oauthService: OAuthService,
        private http: HttpClient,
        private router: Router,
    ) {
        window.addEventListener('storage', (event) => {
            if (event.key !== 'access_token' && event.key !== null) {
                return;
            }
            this.isAuthenticatedSubject$.next(this.oauthService.hasValidAccessToken());
            if (!this.oauthService.hasValidAccessToken()) {
                this.navigateToLoginPage();
            }
        });

        this.oauthService.events
            .subscribe(_ => {
                this.isAuthenticatedSubject$.next(this.oauthService.hasValidAccessToken());
            });
        this.isAuthenticatedSubject$.next(this.oauthService.hasValidAccessToken());

        this.oauthService.events
            .pipe(filter(e => ['token_received'].includes(e.type)))
            .subscribe(e => this.oauthService.loadUserProfile());

        this.oauthService.events
            .pipe(filter(e => ['session_terminated', 'session_error'].includes(e.type)))
            .subscribe(e => this.navigateToLoginPage());

        this.oauthService.setupAutomaticSilentRefresh();
    }

    public canActivateProtectedRoutes$: Observable<boolean> = combineLatest([
        this.isAuthenticated$,
        this.isDoneLoading$
    ]).pipe(map(values => values.every(b => b)));

    private navigateToLoginPage() {
        this.router.navigateByUrl('/login');
    }

    public async runInitialLoginSequence(): Promise<void> {
        return firstValueFrom(this.fetchSsoConfig())
            .then(config => this.oauthService.configure(config))
            .then(() => this.oauthService.loadDiscoveryDocument())
            .then(() => this.oauthService.tryLogin())
            .then(() => {
                if (this.oauthService.hasValidAccessToken()) {
                    if (!this.verifyIdToken(this.oauthService.getIdToken())) {
                        return Promise.reject()
                    }
                    return Promise.resolve();
                }
                return this.oauthService.silentRefresh()
                    .then(() => Promise.resolve())
                    .catch(result => {
                        const errorResponsesRequiringUserInteraction = [
                            'interaction_required',
                            'login_required',
                            'account_selection_required',
                            'consent_required',
                        ];
                        if (result
                            && result.reason
                            && errorResponsesRequiringUserInteraction.indexOf(result.reason.error) >= 0) {
                            return Promise.resolve();
                        }
                        return Promise.reject(result);
                    });
            })

            .then(() => {
                this.isDoneLoadingSubject$.next(true);
                if (this.oauthService.state && this.oauthService.state !== 'undefined' && this.oauthService.state !== 'null') {
                    let stateUrl = this.oauthService.state;
                    if (stateUrl.startsWith('/') === false) {
                        stateUrl = decodeURIComponent(stateUrl);
                    }
                    const redirectUrl = stateUrl && stateUrl.includes('/login') ? '/' : stateUrl
                    this.router.navigateByUrl(redirectUrl);
                }
            })
            .catch(() => this.isDoneLoadingSubject$.next(true));
    }

    fetchSsoConfig() {
        const ssoConfigLocalStorage = localStorage.getItem('ssoConfig')
        if (!ssoConfigLocalStorage) {
            return this.http.get<SsoAuthConfig>(environment.backend + this.resourceUrl).pipe(
                map(ssoConfig => {
                    this.ssoConfig = ssoConfig
                    this.enableSso = true
                    localStorage.setItem('ssoConfig', JSON.stringify(ssoConfig))
                    return this.getAuthConfigFromSsoAuthConfig(ssoConfig)
                }),
            )
        } else {
            this.enableSso = true
            this.ssoConfig = JSON.parse(ssoConfigLocalStorage) as SsoAuthConfig
            const ssoAuthConfig = this.getAuthConfigFromSsoAuthConfig(this.ssoConfig)
            return of(ssoAuthConfig)
        }
    }

    private getAuthConfigFromSsoAuthConfig(ssoConfig: SsoAuthConfig) {
        return {
            issuer: ssoConfig.issuer,
            clientId: ssoConfig.clientId,
            responseType: ssoConfig.responseType,
            scope: ssoConfig.scope,
            redirectUri: ssoConfig.redirectBaseUrl + '/',
            dummyClientSecret: ssoConfig.clientSecret,
            oidc: ssoConfig.oidc,
            useHttpBasicAuth: true,
            postLogoutRedirectUri: ssoConfig.redirectBaseUrl + '/',
            sessionChecksEnabled: true,
            logoutUrl: ssoConfig.redirectBaseUrl + '/',
            customQueryParams: ssoConfig.additionalQueryParams,
            redirectUriAsPostLogoutRedirectUriFallback: true,
            requireHttps: false,
            showDebugInformation: true,
            clearHashAfterLogin: false,
            silentRefreshTimeout: 0,
            useSilentRefresh: true
        } as AuthConfig
    }

    private verifyIdToken(idToken: string): boolean {
        try {
            const decoded = JwtService.decodeToken(idToken)
            const now = Math.floor(Date.now() / 1000);
            if (!decoded.iss || !decoded.sub || !decoded.aud || !decoded.exp || !decoded.iat) return false
            const audienceValid = Array.isArray(decoded.aud)
                ? decoded.aud.includes(this.ssoConfig.clientId)
                : decoded.aud === this.ssoConfig.clientId;
            if (!audienceValid) return false
            if (decoded.iss !== this.ssoConfig.issuer) return false
            if (Array.isArray(decoded.aud) && decoded.aud.length > 1 && !decoded.azp) return false
            if (decoded.azp && decoded.azp !== this.ssoConfig.clientId) return false
            if (now > decoded.exp + this.clockSkew) return false
            return now >= decoded.iat - this.clockSkew;
        } catch (error) {
            return false;
        }
    }

    getSsoProviderName() {
        return this.ssoConfig?.ssoProviderName
    }

    getSsoProviderImageUrl() {
        return this.ssoConfig?.ssoProviderImageUrl
    }

    get accessTokenValid(): boolean {
        return this.oauthService.hasValidAccessToken();
    }

    get tokenEndpoint(): string {
        return this.oauthService.tokenEndpoint;
    }

    get headers() {
        return this.ssoConfig?.headers
    }

    get getEnableSso() {
        return this.enableSso
    }

    public login(targetUrl?: string) {
        this.oauthService.initLoginFlow(targetUrl || this.router.url);
    }

    public logout() {
        this.oauthService.logOut();
    }

    public get accessToken() {
        return this.oauthService.getAccessToken();
    }
}
