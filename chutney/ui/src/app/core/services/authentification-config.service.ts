/*
 * SPDX-FileCopyrightText: 2017-2025 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from "@angular/common/http";
import { Injectable, OnDestroy } from "@angular/core";
import { environment } from "@env/environment";
import { AuthConfig } from "angular-oauth2-oidc";
import { map, Subject, takeUntil } from "rxjs";

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

interface UserPasswordAuthenticationConfig {
    enableUserPassword: boolean,
    enableSso: boolean,
    ssoConfig: SsoAuthConfig
}

@Injectable({
    providedIn: 'root'
})
export class AuthenticationConfigService implements OnDestroy {

    private unsubscribeSub$: Subject<void> = new Subject();

    private resourceUrl = '/api/v1/authentication/config';

    private enableUserPassword: boolean;
    private enableSso: boolean;
    private ssoConfig: SsoAuthConfig;

    constructor(
        private http: HttpClient
    ) {}

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    fetchConfig() {
        return this.http.get<UserPasswordAuthenticationConfig>(environment.backend + this.resourceUrl).pipe(
            takeUntil(this.unsubscribeSub$),
            map(authenticationConfig => {
                this.enableUserPassword = authenticationConfig.enableUserPassword;
                this.enableSso = authenticationConfig.enableSso;
                if (Object.keys(authenticationConfig.ssoConfig).length === 0) {
                    localStorage.removeItem('ssoConfig')
                } else {
                    this.ssoConfig = authenticationConfig.ssoConfig
                    localStorage.setItem('ssoConfig', JSON.stringify(this.ssoConfig))
                    // ?? return this.getAuthConfigFromSsoAuthConfig(this.ssoConfig)
                }
                return authenticationConfig;
            }),
        ).subscribe();
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

    get getEnableUserPassword() {
        return this.enableUserPassword;
    }

    get getEnableSso() {
        return this.enableSso;
    }


}
