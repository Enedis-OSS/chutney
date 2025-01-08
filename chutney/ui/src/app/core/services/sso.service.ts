/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from '@angular/common/http';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '@env/environment';
import { map, tap } from 'rxjs';
import { Injectable } from '@angular/core';


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

    private resourceUrl = '/api/v1/sso/config';
    private ssoConfig: SsoAuthConfig
    private enableSso = false


    constructor(private oauthService: OAuthService, private http: HttpClient) {}

    fetchSsoConfig(): void {
        const ssoConfigLocalStorage = localStorage.getItem('ssoConfig')
        if (!ssoConfigLocalStorage) {
            this.http.get<SsoAuthConfig>(environment.backend + this.resourceUrl).pipe(
                map(ssoConfig => {
                    this.ssoConfig = ssoConfig
                    localStorage.setItem('ssoConfig', JSON.stringify(ssoConfig))
                    return this.getAuthConfigFromSsoAuthConfig(ssoConfig)
                }),
                tap(ssoConfig => this.oauthService.configure(ssoConfig)),
                tap(_ => this.oauthService.stopAutomaticRefresh()),
                tap(_ => this.oauthService.loadDiscoveryDocumentAndTryLogin()),
                tap(_ => this.enableSso = true)
            ).subscribe()
        } else {
            this.enableSso = true
            this.ssoConfig = JSON.parse(ssoConfigLocalStorage) as SsoAuthConfig
            const ssoAuthConfig  = this.getAuthConfigFromSsoAuthConfig(this.ssoConfig)
            this.oauthService.configure(ssoAuthConfig);
            this.oauthService.stopAutomaticRefresh();
            this.oauthService.loadDiscoveryDocumentAndTryLogin();

        }
    }

    login() {
        this.oauthService.initCodeFlow();
    }

    logout() {
        if (this.idToken) {
            this.oauthService.logOut({
                'id_token_hint': this.idToken
            });
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
            useSilentRefresh: false,
            redirectUriAsPostLogoutRedirectUriFallback: true,
            requireHttps: false
        } as AuthConfig
    }

    getSsoProviderName() {
        return this.ssoConfig?.ssoProviderName
    }

    getSsoProviderImageUrl() {
        return this.ssoConfig?.ssoProviderImageUrl
    }

    get accessToken(): string {
        return this.oauthService.getAccessToken();
    }

    get accessTokenValid(): boolean {
        return this.oauthService.hasValidAccessToken();
    }

    get idToken(): string {
        return this.oauthService.getIdToken();
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
}
