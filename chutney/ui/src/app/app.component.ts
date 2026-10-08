/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as moment from 'moment';

@Component({
    selector: 'chutney-main',
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss',
    standalone: false
})
export class AppComponent {


    constructor(private translate: TranslateService) {
        // the language is already selected and loaded by the app initializer
        this.updateMomentLocal(this.translate.currentLang || this.translate.getDefaultLang());
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
