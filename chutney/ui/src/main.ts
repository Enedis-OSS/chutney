/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import '@angular/localize/init';
import { enableProdMode, APP_INITIALIZER, provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';

import { environment } from '@env/environment';
import { provideTranslateService, provideMissingTranslationHandler } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { DefaultMissingTranslationHandler } from '@core/initializer/app.translate.factory';
import { BsModalService, ModalModule } from 'ngx-bootstrap/modal';
import { themeInitializer } from '@core/initializer/theme.initializer';
import { ThemeService } from '@core/theme/theme.service';
import { provideHttpClient, withXhr, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { AppRoutingModule } from './app/app-routing.module';
import { CoreModule } from '@core/core.module';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '@shared/shared.module';
import { OAuthModule } from 'angular-oauth2-oidc';
import { AppComponent } from './app/app.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
    providers: [
        importProvidersFrom(
        BrowserModule, BrowserAnimationsModule, CommonModule, AppRoutingModule, CoreModule,
        FormsModule, ModalModule, NgbModule,
        SharedModule, OAuthModule.forRoot()),
        provideTranslateService({
            loader: provideTranslateHttpLoader({
                prefix: 'assets/i18n/',
                suffix: '.json'
            }),
            missingTranslationHandler: provideMissingTranslationHandler(DefaultMissingTranslationHandler)
        }),
        BsModalService,
        {
            provide: APP_INITIALIZER,
            useFactory: themeInitializer,
            deps: [ThemeService],
            multi: true
        },
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideZoneChangeDetection(),
        provideAnimationsAsync()
    ]
})
  .catch(err => console.error(err));
