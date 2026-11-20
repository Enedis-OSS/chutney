/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { VacuumComponent } from '@modules/vacuum/components/vacuum.component';
import { RouterModule } from '@angular/router';
import { VacuumRoute } from '@modules/vacuum/vacuum.routes';
import { AsyncPipe } from '@angular/common';
import { MoleculesModule } from '../../molecules/molecules.module';
import { NgbNav, NgbNavContent, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
    imports: [
        RouterModule.forChild(VacuumRoute),
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
