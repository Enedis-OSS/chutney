/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';


import { MetricsComponent } from './components/metrics.component';
import { MetricsRoute } from './metrics.routes';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { MoleculesModule } from '@molecules/molecules.module';
import { TranslateDirective, TranslatePipe } from '@ngx-translate/core';

@NgModule({
    imports: [
        CommonModule,
        NgbModule,
        MoleculesModule,
        RouterModule.forChild(MetricsRoute),
        TranslatePipe,
        TranslateDirective,
        MetricsComponent
    ],
    exports: [
        TranslatePipe,
        TranslateDirective
    ],
})
export class MetricsModule {
}
