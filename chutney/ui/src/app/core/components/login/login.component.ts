/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of, Subscription } from 'rxjs';

import { InfoService, LoginService } from '@core/services';
import { SsoService } from '@core/services/sso.service';
import { catchError } from 'rxjs/operators';

@Component({
    selector: 'chutney-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnDestroy, OnInit {

    username: string;
    password: string;
    action: string;

    private forwardUrl: string;
    private paramsSubscription: Subscription;
    private queryParamsSubscription: Subscription;
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
        this.paramsSubscription = this.route.params.subscribe(params => {
            this.action = params['action'];
        });
        this.queryParamsSubscription = this.route.queryParams.subscribe(params => {
            this.forwardUrl = params['url'];
        });
        this.infoService.getVersion().subscribe(result => {
            this.version = result;
        });
        this.infoService.getApplicationName().subscribe(result => {
            this.applicationName = result;
        });
    }

    ngOnInit() {
        if (this.loginService.isAuthenticated()) {
            this.loginService.navigateAfterLogin();
        }
    }

    ngOnDestroy() {
        if (this.paramsSubscription) {
            this.paramsSubscription.unsubscribe();
        }
        if (this.queryParamsSubscription) {
            this.queryParamsSubscription.unsubscribe();
        }
    }

    login() {
        this.loginService.login(this.username, this.password).pipe(
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
