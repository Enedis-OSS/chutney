/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MomentModule } from 'ngx-moment';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from '@shared/shared.module';

import { DatasetListComponent } from './components/dataset-list/dataset-list.component';
import { DatasetRoute } from './dataset.routes';
import { MoleculesModule } from '@molecules/molecules.module';
import { DatasetEditionComponent } from './components/dataset-edition/dataset-edition.component';
import { NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';


const ROUTES = [
    ...DatasetRoute
];

@NgModule({
    imports: [
    CommonModule,
    RouterModule.forChild(ROUTES),
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    NgbModule,
    MomentModule,
    MoleculesModule,
    NgMultiSelectDropDownModule.forRoot(),
    DatasetListComponent,
    DatasetEditionComponent
],
    exports: [
        DatasetEditionComponent,
    ]
})
export class DatasetModule {
}
