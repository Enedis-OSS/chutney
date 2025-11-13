/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { LoginComponent } from '@core/components/login/login.component';
import { Authorization } from '@model';
import { ParentComponent } from '@core/components/parent/parent.component';
import { ChutneyMainHeaderComponent } from '@shared/components/layout/header/chutney-main-header.component';
import { ChutneyLeftMenuComponent } from '@shared/components/layout/left-menu/chutney-left-menu.component';
import { featuresGuard } from '@core/guards/features.guard';
import { authGuard } from '@core/guards';
import { featuresResolver } from '@core/feature/features.resolver';

export const appRoutes: Routes = [
    {path: 'login', component: LoginComponent},
    {path: 'login/:action', component: LoginComponent},
    {
        path: '', component: ParentComponent,
        canActivate: [authGuard],
        resolve: {'features': () => featuresResolver},
        children: [
            {path: '', component: ChutneyMainHeaderComponent, outlet: 'header'},
            {path: '', component: ChutneyLeftMenuComponent, outlet: 'left-side-bar'},
            {path: '', redirectTo: '/scenario', pathMatch: 'full'},
            {
                path: 'scenario',
                loadChildren: () => import('./modules/scenarios/scenario.module').then(m => m.ScenarioModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.SCENARIO_READ, Authorization.EXECUTION_READ]}
            },
            {
                path: 'campaign',
                loadChildren: () => import('./modules/campaign/campaign.module').then(m => m.CampaignModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.CAMPAIGN_READ, Authorization.EXECUTION_READ]}
            },
            {
                path: 'dataset',
                loadChildren: () => import('./modules/dataset/dataset.module').then(m => m.DatasetModule),
                canActivate: [authGuard, featuresGuard], // add requiredAuthorizations
                data: {'authorizations': [Authorization.DATASET_READ]}
            },
            {
                path: 'execution/search',
                loadChildren: () => import('@modules/execution-search/execution-search.module').then(m => m.ExecutionSearchModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.EXECUTION_READ]}
            },
            {
                path: 'admin/vacuum',
                loadChildren: () => import('./modules/vacuum/vacuum.module').then(m => m.VacuumModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.ADMIN_ACCESS]}
            },
            {
                path: 'environments/names',
                loadChildren: () => import('./modules/environment/environment.module').then(m => m.EnvironmentModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.ENVIRONMENT_READ]}
            },
            {
                path: 'environments/targets',
                loadChildren: () => import('./modules/target/target.module').then(m => m.TargetModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.TARGET_READ]}
            },
            {
                path: 'environments/variables',
                loadChildren: () => import('./modules/environment-variable/environment-variable.module').then(m => m.EnvironmentVariableModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.VARIABLE_READ]}
            },
            {
                path: 'admin/agent',
                loadChildren: () => import('./modules/agent-network/agent-network.module').then(m => m.AgentNetworkModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.ADMIN_ACCESS]}
            },
            {
                path: 'admin/plugins',
                loadChildren: () => import('./modules/plugins/plugin-configuration.module').then(m => m.PluginConfigurationModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.ADMIN_ACCESS]}
            },
            {
                path: 'admin/roles',
                loadChildren: () => import('./modules/roles/roles.module').then(m => m.RolesModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.ADMIN_ACCESS]}
            },
            {
                path: 'admin/metrics',
                loadChildren: () => import('./modules/metrics/metrics.module').then(m => m.MetricsModule),
                canActivate: [authGuard],
                data: {'authorizations': [Authorization.ADMIN_ACCESS]}
            }
        ]
    },
    {path: '**', redirectTo: ''}

];

@NgModule({
    imports: [RouterModule.forRoot(appRoutes, { useHash: true, enableTracing: false })],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
