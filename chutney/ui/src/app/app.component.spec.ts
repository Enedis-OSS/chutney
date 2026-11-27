/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TestBed, waitForAsync } from '@angular/core/testing';
import { AppComponent } from './app.component';
import {
    provideTranslateService,
    provideMissingTranslationHandler,
    DefaultMissingTranslationHandler
} from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

describe('AppComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [
        AppComponent
      ],
      providers: [
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                                    prefix: './i18n/',
                                    suffix: '.json'
                                }),
                                missingTranslationHandler: provideMissingTranslationHandler(
                                    DefaultMissingTranslationHandler
                                )
        })
    ]
    }).compileComponents();
  }));
  it('should create the app', () => {

    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  });
});
