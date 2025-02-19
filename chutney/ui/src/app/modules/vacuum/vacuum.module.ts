/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { VacuumComponent } from '@modules/vacuum/components/vacuum.component';
import { RouterModule } from '@angular/router';
import { VacuumRoute } from '@modules/vacuum/vacuum.routes';
import { TranslateModule } from '@ngx-translate/core';
import { AsyncPipe } from '@angular/common';
import { MoleculesModule } from '../../molecules/molecules.module';
import { NgbNav, NgbNavContent, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
    imports: [
        RouterModule.forChild(VacuumRoute),
        TranslateModule,
        AsyncPipe,
        MoleculesModule,
        NgbNav,
        NgbNavContent,
        NgbNavLink,
        NgbNavLinkBase,
    ],
  declarations: [VacuumComponent],
  providers: []
})
export class VacuumModule {
}
