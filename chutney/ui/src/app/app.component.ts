/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, ChangeDetectionStrategy } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import * as moment from 'moment';
import { AlertComponent } from '@shared/components/alert/alert.component';
import { RouterOutlet } from '@angular/router';
import { detectTranslationLang } from '@core/initializer/app.translate.factory';

@Component({
    selector: 'chutney-main',
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [AlertComponent, RouterOutlet]
})
export class AppComponent {


    constructor() {
        registerLocaleData(localeFr);
        this.updateMomentLocal(detectTranslationLang());
    }

    private updateMomentLocal(lang: string) {
        if (chutneyMomentCalendar[lang]) {
            moment.updateLocale(lang, chutneyMomentCalendar[lang]);
        }
    }
}

const chutneyMomentCalendar = {
    'en': {
        calendar: {
            sameDay: '[Today at] HH:mm',
            nextDay: '[Tomorrow at] HH:mm',
            nextWeek: 'dddd [at] HH:mm',
            lastDay: '[Yesterday at] HH:mm',
            lastWeek: '[Last] dddd [at] HH:mm',
            sameElse: 'L [at] HH:mm',
        }
    },
    'fr': {
        calendar: {
            sameDay: '[Aujourd’hui à] HH:mm',
            nextDay: '[Demain à] HH:mm',
            nextWeek: 'dddd [à] HH:mm',
            lastDay: '[Hier à] HH:mm',
            lastWeek: 'dddd [dernier à] HH:mm',
            sameElse: 'L [à] HH:mm',
        }
    }
};
