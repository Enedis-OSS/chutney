/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { APP_INITIALIZER, NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { LoginComponent } from './components/login/login.component';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ParentComponent } from './components/parent/parent.component';
import { DROPDOWN_SETTINGS, DropdownSettings } from '@core/model/dropdown-settings';
import { OAuth2ContentTypeInterceptor } from '@core/services/oauth2-content-type-interceptor.service';
import { AuthInterceptor, TokenInterceptor } from '@core/services/auth.interceptor';
import { authAppInitializerFactory } from '@core/services/auth.app.initializer.factory';
import { SsoService } from '@core/services/sso.service';

@NgModule({
    declarations: [
        LoginComponent,
        ParentComponent,
    ],
    imports: [
        CommonModule,
        FormsModule,
        HttpClientModule,
        RouterModule,
        SharedModule,
        TranslateModule
    ],
    providers: [
        {provide: APP_INITIALIZER, useFactory: authAppInitializerFactory, deps: [SsoService], multi: true},
        {provide: DROPDOWN_SETTINGS, useClass: DropdownSettings},
        {provide: HTTP_INTERCEPTORS, useClass: OAuth2ContentTypeInterceptor, multi: true },
        {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true},
        {provide: HTTP_INTERCEPTORS, useClass: TokenInterceptor, multi: true},
    ]

})
export class CoreModule { }
