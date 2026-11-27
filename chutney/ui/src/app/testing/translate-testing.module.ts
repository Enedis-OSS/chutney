/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TranslateLoader, TranslatePipe, TranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { DefaultMissingTranslationHandler } from '@core/initializer/app.translate.factory';
import {
  provideTranslateService,
  provideMissingTranslationHandler
} from '@ngx-translate/core';

import { EventEmitter, Injectable, NgModule, Pipe, PipeTransform } from '@angular/core';
import { Observable, of } from 'rxjs';

import {TRANSLATIONS_EN} from '../../assets/i18n/i18n.spec';

export class FakeLoader implements TranslateLoader {
    getTranslation(lang: string): Observable<any> {
        return of(TRANSLATIONS_EN);
    }
}

@Pipe({
    standalone: false,
    name: 'translate'
})
export class TranslatePipeMock implements PipeTransform {
    public name = 'translate';

    public transform(query: string, ...args: any[]): any {
        return fromKey(query);
    }
}

@Injectable()
export class TranslateServiceStub {
    public get(key: string): Observable<string> {
        return of(fromKey(key));
    }
    public onLangChange: EventEmitter<any> = new EventEmitter();
    public onTranslationChange: EventEmitter<any> = new EventEmitter();
    public onDefaultLangChange: EventEmitter<any> = new EventEmitter();
}

function fromKey(key: string): string {
    let r: any = TRANSLATIONS_EN;
    key.split('.').forEach((s) => { if (r) r = r[s]; });
    if (r) { return r; } else { return key; }
}

@NgModule({
    declarations: [
        TranslatePipeMock
    ],
    providers: [
        { provide: TranslateService, useClass: TranslateServiceStub },
        { provide: TranslatePipe, useClass: TranslatePipeMock },
        provideTranslateService({
                    loader: provideTranslateHttpLoader({
                        prefix: './i18n/',
                        suffix: '.json'
                    }),
                    missingTranslationHandler: provideMissingTranslationHandler(
                        DefaultMissingTranslationHandler
                    )
                }),
    ],
    exports: [
        TranslatePipeMock
    ]
})
export class TranslateTestingModule {

}
