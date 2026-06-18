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
import { MoleculesModule } from '../../molecules/molecules.module';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule, NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { EnvironmentImportComponent } from './import/environment-import.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';


@NgModule({
    declarations: [
        EnvironmentsComponent,
        EnvironmentImportComponent
    ],
    imports: [
        CommonModule,
        RouterModule.forChild(environmentsRoutes),
        FormsModule,
        ReactiveFormsModule,
        MoleculesModule,
        TranslateModule,
        NgbModule,
        NgbTooltipModule
    ]
})
export class EnvironmentModule {
}
