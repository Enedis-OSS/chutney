/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from '@angular/common/http';
import { AuthConfig, OAuthErrorEvent, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '@env/environment';
import { BehaviorSubject, combineLatest, firstValueFrom, map, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';


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


    constructor(
        private oauthService: OAuthService,
        private http: HttpClient,
        private router: Router,
    ) {
        this.oauthService.events.subscribe(event => {
            if (event instanceof OAuthErrorEvent) {
                console.error('OAuthErrorEvent Object:', event);
            } else {
                console.warn('OAuthEvent Object:', event);
            }
        });
        window.addEventListener('storage', (event) => {
            if (event.key !== 'access_token' && event.key !== null) {
                return;
            }

            console.warn('Noticed changes to access_token (most likely from another tab), updating isAuthenticated');
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

    fetchSsoConfig() {
        const ssoConfigLocalStorage = localStorage.getItem('ssoConfig')
        if (!ssoConfigLocalStorage) {
            return this.http.get<SsoAuthConfig>(environment.backend + this.resourceUrl).pipe(
                map(ssoConfig => {
                    this.ssoConfig = ssoConfig
                    localStorage.setItem('ssoConfig', JSON.stringify(ssoConfig))
                    return this.getAuthConfigFromSsoAuthConfig(ssoConfig)
                }),
                //tap(ssoConfig => this.oauthService.configure(ssoConfig)),
                //tap(_ => this.oauthService.stopAutomaticRefresh()),
                //tap(_ => this.oauthService.loadDiscoveryDocumentAndTryLogin()),
                //tap(_ => this.enableSso = true)
            )//.subscribe()
        } else {
            this.enableSso = true
            this.ssoConfig = JSON.parse(ssoConfigLocalStorage) as SsoAuthConfig
            const ssoAuthConfig = this.getAuthConfigFromSsoAuthConfig(this.ssoConfig)
            return of(ssoAuthConfig)
            //this.oauthService.configure(ssoAuthConfig);
            //this.oauthService.stopAutomaticRefresh();
            //this.oauthService.loadDiscoveryDocumentAndTryLogin();

        }
    }

    //login() {
    //    this.oauthService.loadDiscoveryDocument().then((doc) => {
    //        this.oauthService.tryLoginCodeFlow({preventClearHashAfterLogin: true}).catch(err => {
    //            console.error(err);
    //        }).then(() => {
    //            this.oauthService.initImplicitFlow()
    //        });
    //    });
    //    //this.oauthService.initCodeFlow();
    //}

    //logout() {
    //    if (this.idToken) {
    //        this.oauthService.logOut({
    //            'id_token_hint': this.idToken
    //        });
    //    }
    //}

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
            silentRefreshTimeout: 1000,
            useSilentRefresh: true
        } as AuthConfig
    }

    getSsoProviderName() {
        return this.ssoConfig?.ssoProviderName
    }

    getSsoProviderImageUrl() {
        return this.ssoConfig?.ssoProviderImageUrl
    }

    //get accessToken(): string {
    //    return this.oauthService.getAccessToken();
    //}

    get accessTokenValid(): boolean {
        return this.oauthService.hasValidAccessToken();
    }

    //get idToken(): string {
    //    return this.oauthService.getIdToken();
    //}

    get tokenEndpoint(): string {
        return this.oauthService.tokenEndpoint;
    }

    get headers() {
        return this.ssoConfig?.headers
    }

    get getEnableSso() {
        return this.enableSso
    }

    public canActivateProtectedRoutes$: Observable<boolean> = combineLatest([
        this.isAuthenticated$,
        this.isDoneLoading$
    ]).pipe(map(values => values.every(b => b)));

    private navigateToLoginPage() {
        // TODO: Remember current URL
        this.router.navigateByUrl('/login');
    }

    public runInitialLoginSequence(): Promise<void> {
        if (location.hash) {
            console.log('Encountered hash fragment, plotting as table...');
            console.table(location.hash.substr(1).split('&').map(kvp => kvp.split('=')));
        }
        return firstValueFrom(this.fetchSsoConfig()).then(config => {
            this.oauthService.configure(config)
        })
            .then(() => this.oauthService.loadDiscoveryDocument())
            .then(() => this.oauthService.tryLogin())
            .then(() => {
                if (this.oauthService.hasValidAccessToken()) {
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
                            console.warn('User interaction is needed to log in, we will wait for the user to manually log in.');
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
                    console.log(`There was state of ${this.oauthService.state}, so we are sending you to: ${stateUrl}`);
                    this.router.navigateByUrl('/scenario');
                }
            })
            .catch(() => this.isDoneLoadingSubject$.next(true));
    }

    public login(targetUrl?: string) {
        this.oauthService.initLoginFlow(targetUrl || this.router.url);
    }

    public logout() {
        this.oauthService.logOut();
    }

    public refresh() {
        this.oauthService.silentRefresh();
    }

    public hasValidToken() {
        return this.oauthService.hasValidAccessToken();
    }

    // These normally won't be exposed from a service like this, but
    // for debugging it makes sense.
    public get accessToken() {
        return this.oauthService.getAccessToken();
    }

    public get refreshToken() {
        return this.oauthService.getRefreshToken();
    }

    public get identityClaims() {
        return this.oauthService.getIdentityClaims();
    }

    public get idToken() {
        return this.oauthService.getIdToken();
    }

    public get logoutUrl() {
        return this.oauthService.logoutUrl;
    }
}
