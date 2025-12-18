/*
 * SPDX-FileCopyrightText: 2017-2025 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { HttpClient } from "@angular/common/http";
import { Injectable, OnDestroy } from "@angular/core";
import { environment } from "@env/environment";
import { map, Subject, takeUntil } from "rxjs";

interface UserPasswordAuthenticationConfig {
    userPassword: boolean
}

@Injectable({
    providedIn: 'root'
})
export class UserPasswordAuthenticationService implements OnDestroy {

    private unsubscribeSub$: Subject<void> = new Subject();

    private resourceUrl = '/api/v1/authentication/config';

    private userPasswordAuthenticationActive: boolean;

    constructor(
        private http: HttpClient
    ) {

    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    fetchConfig() {
        return this.http.get<UserPasswordAuthenticationConfig>(environment.backend + this.resourceUrl).pipe(
            takeUntil(this.unsubscribeSub$),
            map(authenticationConfig => {
                this.userPasswordAuthenticationActive = authenticationConfig.userPassword;
                return authenticationConfig;
            }),
        ).subscribe();
    }

    get getUserPasswordAuthenticationActive() {
        return this.userPasswordAuthenticationActive;
    }


}
