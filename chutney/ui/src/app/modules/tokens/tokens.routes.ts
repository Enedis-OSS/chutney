/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Routes } from '@angular/router';

import { authGuard } from '@core/guards';
import { Authorization } from '@model';
import { TokenListComponent } from '@modules/tokens/list/tokens.component';

export const tokensRoutes: Routes = [
    {
        path: '',
        component: TokenListComponent,
        canActivate: [authGuard],
        data: { 'authorizations': [ Authorization.ADMIN_ACCESS, Authorization.CAMPAIGN_WRITE, Authorization.DATASET_WRITE, Authorization.DATASET_READ, Authorization.SCENARIO_WRITE, Authorization.SCENARIO_READ, Authorization.ENVIRONMENT_READ ] }
    }
];
