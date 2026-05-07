/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { SharedModule } from '@shared/shared.module';

import { CoreModule } from '@core/core.module';
import { tokensRoutes } from '@modules/tokens/tokens.routes';
import { TokenCreationComponent } from './creation/tokens-creation.component';
import { TokenListComponent } from './list/tokens.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TokenService } from '@core/services/token.service';

@NgModule({
    imports: [
        CommonModule,
        RouterModule.forChild(tokensRoutes),
        FormsModule,
        ReactiveFormsModule,
        SharedModule,
        CoreModule,
        NgbModule,
        TranslateModule,
    ],
    declarations: [
        TokenListComponent,
        TokenCreationComponent
    ],
    providers: [
        TokenService
    ]
})
export class TokenModule {
}
