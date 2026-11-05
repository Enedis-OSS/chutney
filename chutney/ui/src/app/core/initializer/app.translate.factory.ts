/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { HttpClient } from '@angular/common/http';
import { MissingTranslationHandler, MissingTranslationHandlerParams, TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

export const DEFAULT_TRANSLATION_LANG = 'en';

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

/**
 * Selects the language of the browser, falling back on the default one when it is not usable.
 */
export function detectTranslationLang(): string {
  const browserLang = typeof navigator === 'undefined' ? undefined : navigator.language;
  return browserLang?.substring(0, 2) || DEFAULT_TRANSLATION_LANG;
}

/**
 * Selects and loads the translations of the application.
 * <p>
 * The returned promise settles once both the fallback language and the language asked by the browser
 * are available, so that any service resolving a translation synchronously - the login error messages
 * for instance - never reads an empty translation store.
 */
export async function loadTranslations(translateService: TranslateService): Promise<void> {
  const lang = detectTranslationLang();
  // the fallback language is loaded first so that `setDefaultLang` resolves it
  // instead of requesting the very same file a second time
  await firstValueFrom(translateService.getTranslation(DEFAULT_TRANSLATION_LANG)).catch(() => ({}));
  translateService.setDefaultLang(DEFAULT_TRANSLATION_LANG);
  await firstValueFrom(translateService.use(lang)).catch(() => ({}));
}

export class DefaultMissingTranslationHandler implements MissingTranslationHandler {
    handle(params: MissingTranslationHandlerParams) {
        return params.key + '*';
    }
}
