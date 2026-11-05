/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TestBed } from '@angular/core/testing';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { Observable, Subject } from 'rxjs';

import { DEFAULT_TRANSLATION_LANG, DefaultMissingTranslationHandler, detectTranslationLang, loadTranslations } from './app.translate.factory';

import TRANSLATIONS_EN from 'src/assets/i18n/en.json';

/**
 * Loader letting the test decide when the translations are served.
 */
class ControllableLoader implements TranslateLoader {

    static pending: Subject<any>[] = [];
    static requested: string[] = [];

    static reset() {
        ControllableLoader.pending = [];
        ControllableLoader.requested = [];
    }

    static serve(translations: any) {
        ControllableLoader.pending.forEach(subject => {
            subject.next(translations);
            subject.complete();
        });
    }

    getTranslation(lang: string): Observable<any> {
        ControllableLoader.requested.push(lang);
        const subject = new Subject<any>();
        ControllableLoader.pending.push(subject);
        return subject.asObservable();
    }
}

describe('loadTranslations', () => {

    beforeEach(() => {
        ControllableLoader.reset();
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot({
                loader: {provide: TranslateLoader, useClass: ControllableLoader},
                missingTranslationHandler: {provide: MissingTranslationHandler, useClass: DefaultMissingTranslationHandler}
            })]
        });
    });

    it('should settle only once the translations are loaded', async () => {
        const translateService = TestBed.inject(TranslateService);

        let settled = false;
        const loadPromise = loadTranslations(translateService).then(() => settled = true);
        await Promise.resolve();

        expect(ControllableLoader.requested).toEqual([DEFAULT_TRANSLATION_LANG]);
        expect(settled).toBeFalse();
        // translations are not available yet, the missing translation handler is used as a fallback
        expect(translateService.instant('login.expired')).toBe('login.expired*');

        ControllableLoader.serve(TRANSLATIONS_EN);
        await loadPromise;

        expect(settled).toBeTrue();
        expect(translateService.instant('login.expired')).toBe(TRANSLATIONS_EN['login']['expired']);
    });

    it('should not request the fallback language twice when the browser already uses it', async () => {
        const translateService = TestBed.inject(TranslateService);

        const loadPromise = loadTranslations(translateService);
        await Promise.resolve();
        ControllableLoader.serve(TRANSLATIONS_EN);
        await loadPromise;

        if (detectTranslationLang() === DEFAULT_TRANSLATION_LANG) {
            expect(ControllableLoader.requested).toEqual([DEFAULT_TRANSLATION_LANG]);
        } else {
            expect(ControllableLoader.requested).toEqual([DEFAULT_TRANSLATION_LANG, detectTranslationLang()]);
        }
    });

    it('should detect the language of the browser', () => {
        expect(detectTranslationLang()).toBe(navigator.language.substring(0, 2) || DEFAULT_TRANSLATION_LANG);
    });
});