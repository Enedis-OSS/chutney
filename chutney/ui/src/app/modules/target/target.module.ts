/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { SharedModule } from '@shared/shared.module';
import { MoleculesModule } from '../../molecules/molecules.module';

import { FileSaverModule } from 'ngx-filesaver';
import { targetsRoutes } from '@modules/target/targets.routes';
import { TargetsComponent } from '@modules/target/list/targets.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TargetComponent } from './details/target.component';
import { CoreModule } from '@core/core.module';
import { TooltipModule } from 'ngx-bootstrap/tooltip';

@NgModule({
    imports: [
    CommonModule,
    RouterModule.forChild(targetsRoutes),
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    CoreModule,
    MoleculesModule,
    FileSaverModule,
    NgbModule,
    TooltipModule,
    TargetsComponent,
    TargetComponent
],
})
export class TargetModule {
}
