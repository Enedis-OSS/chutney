/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { MissingTranslationHandler, MissingTranslationHandlerParams, TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

export const DEFAULT_TRANSLATION_LANG = 'en';

export function detectTranslationLang(): string {
    const browserLang =
        typeof navigator === 'undefined' ? undefined : navigator.language;

    return browserLang?.substring(0, 2) || DEFAULT_TRANSLATION_LANG;
}

export async function loadTranslations(translateService: TranslateService): Promise<void> {
    const lang = detectTranslationLang();

    await firstValueFrom(
        translateService.setFallbackLang(DEFAULT_TRANSLATION_LANG)
    );

    await firstValueFrom(
        translateService.use(lang)
    );
}

export class DefaultMissingTranslationHandler implements MissingTranslationHandler {
    handle(params: MissingTranslationHandlerParams) {
        return params.key + '*';
    }
}
