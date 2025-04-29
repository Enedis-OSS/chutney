/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, Subscription } from 'rxjs';

import { InfoService, LoginService } from '@core/services';
import { SsoService } from '@core/services/sso.service';
import { catchError, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'chutney-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnDestroy, OnInit {

    username: string;
    password: string;
    action: string;

    private unsubscribeSub$: Subject<void> = new Subject();
    private forwardUrl: string;
    loginService: LoginService
    version = '';
    applicationName = '';

    constructor(
        loginService: LoginService,
        private infoService: InfoService,
        private route: ActivatedRoute,
        private ssoService: SsoService
    ) {
        this.loginService = loginService
        this.route.params
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(params => this.action = params['action']);
        this.route.queryParams
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(params => this.forwardUrl = params['url']);
        this.infoService.getVersion()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(result => this.version = result);
        this.infoService.getApplicationName()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(result => this.applicationName = result);
    }

    ngOnInit() {
        if (this.loginService.isAuthenticated()) {
            this.loginService.navigateAfterLogin();
        }
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    login() {
        this.loginService.login(this.username, this.password).pipe(
            takeUntil(this.unsubscribeSub$),
            catchError((err => {
                    this.loginService.connectionErrorMessage = err.error;
                    this.action = null;
                    return of(null)
                })
            ))
            .subscribe(
                (user) => {
                    this.loginService.navigateAfterLogin(this.forwardUrl);
                }
            );
    }

    connectSso() {
        this.ssoService.login(this.forwardUrl)
    }

    getSsoProviderName() {
        return this.ssoService.getSsoProviderName()
    }

    displaySsoButton() {
        return this.ssoService.getEnableSso
    }

    getSsoProviderImageUrl() {
        return this.ssoService.getSsoProviderImageUrl()
    }
}
