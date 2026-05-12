/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Routes } from '@angular/router';

import { Authorization } from '@model';
import { TokenListComponent } from '@modules/tokens/list/tokens.component';
import { TokenCreationComponent } from './creation/tokens-creation.component';
import { authGuard } from '@core/guards';
import { TokenDisplayComponent } from './display/tokens-display.component';

export const tokensRoutes: Routes = [
    {
        path: '',
        component: TokenListComponent,
        canActivate: [authGuard],
        data: { 'authorizations': [ Authorization.TARGET_READ ] }
    },
    {
        path: 'creation',
        component: TokenCreationComponent,
        canActivate: [authGuard],
        data: { 'authorizations': [ Authorization.CAMPAIGN_READ ] }
    },
    {
        path: 'display',
        component: TokenDisplayComponent,
        canActivate: [authGuard],
        data: { 'authorizations': [ Authorization.CAMPAIGN_READ ] }
    }
];
