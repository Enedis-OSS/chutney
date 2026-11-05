/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TranslateService } from '@ngx-translate/core';

import { loadTranslations } from '@core/initializer/app.translate.factory';
import { SsoService } from '@core/services/sso.service';

/**
 * Translations are loaded before any other call to the server so that the services resolving a
 * translation during the initial login sequence do not fall back on the missing translation handler.
 */
export function authAppInitializerFactory(translateService: TranslateService, ssoService: SsoService): () => Promise<void> {
    return async () => {
        await loadTranslations(translateService);
        await ssoService.runInitialLoginSequence();
    };
}
