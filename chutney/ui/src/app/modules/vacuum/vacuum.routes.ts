/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Routes } from '@angular/router';
import { VacuumComponent } from '@modules/vacuum/components/vacuum.component';

export const VacuumRoute: Routes = [
    {
        path: '',
        component: VacuumComponent
    }
];
