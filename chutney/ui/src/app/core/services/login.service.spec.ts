/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';

import { DefaultMissingTranslationHandler, loadTranslations } from '@core/initializer/app.translate.factory';
import { FakeLoader } from '../../testing/translate-testing.module';
import { LoginService } from './login.service';
import { SsoService } from './sso.service';

import TRANSLATIONS_EN from 'src/assets/i18n/en.json';

describe('LoginService', () => {

    const routerStub = {
        navigate: jasmine.createSpy('navigate'),
        navigateByUrl: jasmine.createSpy('navigateByUrl'),
        url: '/'
    };

    const ssoServiceStub = {
        accessToken: null,
        get accessTokenValid() {
            return false;
        }
    };

    const encodeToken = (payload: object) => `header.${btoa(JSON.stringify(payload))}.signature`;

    const expiredToken = encodeToken({
        sub: 'jdoe',
        iat: 0,
        exp: 1,
        authorizations: []
    });

    beforeEach(async () => {
        localStorage.clear();
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot({
                loader: {provide: TranslateLoader, useClass: FakeLoader},
                missingTranslationHandler: {provide: MissingTranslationHandler, useClass: DefaultMissingTranslationHandler}
            })],
            providers: [
                LoginService,
                {provide: Router, useValue: routerStub},
                {provide: SsoService, useValue: ssoServiceStub},
                provideHttpClient()
            ]
        });
        // bootstrap the translations the way the app initializer does, before the service is created
        await loadTranslations(TestBed.inject(TranslateService));
    });

    afterEach(() => localStorage.clear());

    it('should translate the session expired message when the language is already loaded', () => {
        const loginService = TestBed.inject(LoginService);
        localStorage.setItem('jwt', expiredToken);

        loginService.isAuthorized('/scenario', {} as any, {} as any).subscribe();

        expect(loginService.connectionErrorMessage).toBe(TRANSLATIONS_EN['login']['expired']);
    });

    it('should translate the sso user not found message when the language is already loaded', () => {
        const loginService = TestBed.inject(LoginService);

        expect(loginService.ssoUserNotFoundMessage).toBe(TRANSLATIONS_EN['login']['sso']['userNotFound']);
    });
});