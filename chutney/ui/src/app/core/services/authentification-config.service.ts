/*
 * SPDX-FileCopyrightText: 2017-2025 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from "@angular/common/http";
import { Injectable, OnDestroy } from "@angular/core";
import { environment } from "@env/environment";
import { BehaviorSubject, Observable, switchMap } from "rxjs";
import { shareReplay } from "rxjs/operators";

export interface SsoAuthConfig {
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

interface AuthenticationConfig {
    enableUserPassword: boolean,
    enableSso: boolean,
    ssoOpenIdConnectConfigDto: SsoAuthConfig
}

@Injectable({
    providedIn: 'root'
})
export class AuthenticationConfigService implements OnDestroy {

    private readonly authenticationSubject$ = new BehaviorSubject<void>(undefined);
    
    private resourceUrl = '/api/v1/authentication/config';

    readonly authenticationConfig$: Observable<AuthenticationConfig> = this.authenticationSubject$.pipe(
        switchMap(() => this.http.get<AuthenticationConfig>(environment.backend + this.resourceUrl)),
        shareReplay({ bufferSize: 1, refCount: false })
    );

    constructor(
        private http: HttpClient
    ) {}

    ngOnDestroy(): void {
        this.authenticationSubject$.next();
        this.authenticationSubject$.complete();
    }


}
