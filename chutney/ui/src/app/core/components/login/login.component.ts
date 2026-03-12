/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of, Subject } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';

import { InfoService, LoginService } from '@core/services';
import { SsoService } from '@core/services/sso.service';
import { AuthenticationConfigService } from '@core/services/authentification-config.service';
import { ThemeService } from '@core/theme/theme.service';

@Component({
    selector: 'chutney-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    standalone: false
})
export class LoginComponent implements OnInit, OnDestroy {
    username = '';
    password = '';
    action = '';

    version = '';
    applicationName = '';

    enableUserPassword = false;
    enableSso = false;
    showUserPassword = false;

    private readonly destroy$ = new Subject<void>();
    private forwardUrl = '';

    constructor(
        public loginService: LoginService,
        private readonly infoService: InfoService,
        private readonly route: ActivatedRoute,
        private readonly ssoService: SsoService,
        private readonly authenticationConfigService: AuthenticationConfigService,
        public readonly themeService: ThemeService
    ) {
        this.route.params
            .pipe(takeUntil(this.destroy$))
            .subscribe(params => {
                this.action = params['action'] ?? '';
            });

        this.route.queryParams
            .pipe(takeUntil(this.destroy$))
            .subscribe(params => {
                this.forwardUrl = params['url'] ?? '';
            });

        this.infoService.getVersion()
            .pipe(takeUntil(this.destroy$))
            .subscribe(version => {
                this.version = version;
            });

        this.infoService.getApplicationName()
            .pipe(takeUntil(this.destroy$))
            .subscribe(applicationName => {
                this.applicationName = applicationName;
            });
    }

    ngOnInit(): void {
        if (this.loginService.isAuthenticated()) {
            this.loginService.navigateAfterLogin();
            return;
        }

        this.authenticationConfigService.authenticationConfig$
            .pipe(takeUntil(this.destroy$))
            .subscribe(config => {
                this.enableUserPassword = config.enableUserPassword;
                this.enableSso = config.enableSso;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    login(): void {
        this.loginService.login(this.username, this.password)
            .pipe(
                takeUntil(this.destroy$),
                catchError(err => {
                    this.loginService.connectionErrorMessage = err?.error ?? 'Login failed';
                    this.action = '';
                    return of(null);
                })
            )
            .subscribe(user => {
                if (user) {
                    this.loginService.navigateAfterLogin(this.forwardUrl);
                }
            });
    }

    connectSso(): void {
        this.ssoService.login(this.forwardUrl);
    }

    toggleLoginMethod(showUserPassword: boolean): void {
        this.showUserPassword = showUserPassword;
    }

    get backgroundImage(): string {
        const theme = this.themeService.isLight() ? 'light' : 'dark';
        return `url(/assets/img/login-${theme}.png)`;
    }

    get ssoProviderName(): string {
        return this.ssoService.getSsoProviderName();
    }

    get ssoProviderImageUrl(): string {
        return this.ssoService.getSsoProviderImageUrl();
    }

    get hasSsoProviderImage(): boolean {
        return !!this.ssoProviderImageUrl;
    }

    get shouldShowUserPasswordInputs(): boolean {
        return (!this.enableSso && this.enableUserPassword) || this.showUserPassword;
    }

    get hasMissingAuthConfig(): boolean {
        return !this.enableUserPassword && !this.enableSso;
    }

    get shouldShowSso(): boolean {
        return this.enableSso && !this.showUserPassword;
    }

    get canSwitchLoginMethod(): boolean {
        return this.enableSso && this.enableUserPassword;
    }

    get shouldShowSsoLogout(): boolean {
        return this.loginService.connectionErrorMessage === this.loginService.ssoUserNotFoundMessage;
    }
}
