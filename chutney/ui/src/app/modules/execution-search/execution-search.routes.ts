/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Routes } from '@angular/router';
import { ExecutionSearchComponent } from './components/execution-search.component';

export const ExecutionSearchRoute: Routes = [
    {
        path: '',
        component: ExecutionSearchComponent
    }
];
