/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { ExecutionSearchRoute } from './execution-search.routes';

import { MoleculesModule } from '../../molecules/molecules.module';
import { ExecutionSearchComponent } from './components/execution-search.component';
import { DateFormatPipe, MomentModule } from 'ngx-moment';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import {
    ExecutionSearchListComponent
} from './components/resultExecutionSearchList/execution-search-list.component';
import { NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(ExecutionSearchRoute),
    FormsModule,
    TranslateModule,
    MoleculesModule,
    MomentModule,
    NgbModule,
    FormsModule,
    ReactiveFormsModule,
    NgMultiSelectDropDownModule.forRoot(),
  ],
  declarations: [ExecutionSearchComponent, ExecutionSearchListComponent],
  providers: [DateFormatPipe]
})
export class ExecutionSearchModule {
}
