/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnvironmentsComponent } from './list/environments.component';
import { RouterModule } from '@angular/router';
import { environmentsRoutes } from '@modules/environment/environments.routes';
import { MoleculesModule } from '@molecules/molecules.module';
import { NgbModule, NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { EnvironmentImportComponent } from './import/environment-import.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TranslateDirective, TranslatePipe } from '@ngx-translate/core';


@NgModule({
    imports: [
        CommonModule,
        RouterModule.forChild(environmentsRoutes),
        FormsModule,
        ReactiveFormsModule,
        MoleculesModule,
        NgbModule,
        NgbTooltipModule,
        TranslatePipe,
        TranslateDirective,
        EnvironmentsComponent,
        EnvironmentImportComponent
    ],
    exports: [
        TranslatePipe,
        TranslateDirective
    ]
})
export class EnvironmentModule {
}
