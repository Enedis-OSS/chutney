/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Authorization } from '@model';
import { MenuItem } from '@shared/components/layout/menuItem';

export const allMenuItems: MenuItem [] = [
    {
        label: '',
        children: [
            {
                label: 'menu.principal.scenarios',
                link: '/scenario',
                iconClass: 'fa fa-film',
                authorizations: [Authorization.SCENARIO_READ, Authorization.EXECUTION_READ]
            },
            {
                label: 'menu.principal.campaigns',
                link: '/campaign',
                iconClass: 'fa fa-flask',
                authorizations: [Authorization.CAMPAIGN_READ, Authorization.EXECUTION_READ]
            },
            {
                label: 'menu.principal.dataset',
                link: '/dataset',
                iconClass: 'fa fa-table',
                authorizations: [Authorization.DATASET_READ]
            },
            {
                label: 'menu.principal.executionSearch',
                link: '/execution/search',
                iconClass: 'fa fa-clipboard',
                authorizations: [Authorization.EXECUTION_READ]
            },
        ],
    },
    {
        label: 'Admin',
        children: [
            {
                label: 'menu.principal.environments',
                link: '/environments/names',
                iconClass: 'fa fa-brands fa-envira',
                authorizations: [Authorization.ENVIRONMENT_READ]
            },
            {
                label: 'menu.principal.targets',
                link: '/environments/targets',
                iconClass: 'fa fa-bullseye',
                authorizations: [Authorization.TARGET_READ]
            },
            {
                label: 'menu.principal.envVariable',
                link: '/environments/variables',
                iconClass: 'fa fa-key',
                authorizations: [Authorization.VARIABLE_READ]
            },
            {
                label: 'menu.principal.plugins',
                link: '/admin/plugins',
                iconClass: 'fa fa-cogs',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.roles',
                link: '/admin/roles',
                iconClass: 'fa fa-user-shield',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.vacuum',
                link: '/admin/vacuum',
                iconClass: 'fa fa-database',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.workers',
                link: '/admin/agent',
                iconClass: 'fa fa-bars',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.previewReport',
                link: '/scenario/report-preview',
                iconClass: 'fa fa-clipboard',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.metrics',
                link: '/admin/metrics',
                iconClass: 'fa fa-chart-simple',
                authorizations: [Authorization.ADMIN_ACCESS]
            }
        ]
    }

];
