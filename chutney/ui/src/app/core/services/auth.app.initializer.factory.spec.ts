/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { of, Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { SsoService } from '@core/services/sso.service';
import { authAppInitializerFactory } from './auth.app.initializer.factory';

describe('authAppInitializerFactory', () => {

    it('should wait for translations before starting the SSO initialization', async () => {
        const fallbackLoaded = new Subject<any>();
        const languageLoaded = new Subject<any>();

        const translateService = jasmine.createSpyObj<TranslateService>(
            'TranslateService',
            ['setFallbackLang', 'use']
        );

        translateService.setFallbackLang.and.returnValue(
            fallbackLoaded.asObservable() as any
        );

        translateService.use.and.returnValue(
            languageLoaded.asObservable() as any
        );

        const ssoService = jasmine.createSpyObj<SsoService>(
            'SsoService',
            ['runInitialLoginSequence']
        );

        ssoService.runInitialLoginSequence.and.returnValue(
            Promise.resolve()
        );

        const initializer = authAppInitializerFactory(
            translateService,
            ssoService
        );

        const initialization = initializer();

        await Promise.resolve();

        expect(translateService.setFallbackLang)
            .toHaveBeenCalledWith('en');

        expect(translateService.use).not.toHaveBeenCalled();

        expect(ssoService.runInitialLoginSequence)
            .not.toHaveBeenCalled();

        fallbackLoaded.next({});
        fallbackLoaded.complete();

        await Promise.resolve();

        expect(translateService.use).toHaveBeenCalled();

        expect(ssoService.runInitialLoginSequence)
            .not.toHaveBeenCalled();

        languageLoaded.next({});
        languageLoaded.complete();

        await initialization;

        expect(ssoService.runInitialLoginSequence)
            .toHaveBeenCalledTimes(1);
    });
});
