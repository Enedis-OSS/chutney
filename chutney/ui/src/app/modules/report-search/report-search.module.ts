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

import { ReportSearchRoute } from './report-search.routes';

import { MoleculesModule } from '../../molecules/molecules.module';
import { ReportSearchComponent } from './components/report-search.component';
import { DateFormatPipe, MomentModule } from 'ngx-moment';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import {
    ReportSearchExecutionReportListComponent
} from './components/resultReportList/report-search-report-list.component';
import { NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(ReportSearchRoute),
    FormsModule,
    TranslateModule,
    MoleculesModule,
    MomentModule,
    NgbModule,
    FormsModule,
    ReactiveFormsModule,
    NgMultiSelectDropDownModule.forRoot(),
  ],
  declarations: [ReportSearchComponent, ReportSearchExecutionReportListComponent],
  providers: [DateFormatPipe]
})
export class ReportSearchModule {
}
