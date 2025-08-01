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
                authorizations: [Authorization.SCENARIO_READ,Authorization.SCENARIO_WRITE,Authorization.SCENARIO_EXECUTE]
            },
            {
                label: 'menu.principal.campaigns',
                link: '/campaign',
                iconClass: 'fa fa-flask',
                authorizations: [Authorization.CAMPAIGN_READ, Authorization.CAMPAIGN_WRITE,Authorization.CAMPAIGN_EXECUTE]
            },
            {
                label: 'menu.principal.dataset',
                link: '/dataset',
                iconClass: 'fa fa-table',
                authorizations: [Authorization.DATASET_READ,Authorization.DATASET_WRITE]
            },
            {
                label: 'menu.principal.executionSearch',
                link: '/execution/search',
                iconClass: 'fa fa-clipboard',
                authorizations: [Authorization.SCENARIO_READ]
            },
        ],
    },
    {
        label: 'Admin',
        children: [
            {
                label: 'menu.principal.environments',
                link: '/environments',
                iconClass: 'fa fa-brands fa-envira',
                authorizations: [Authorization.ENVIRONMENT_ACCESS]
            },
            {
                label: 'menu.principal.targets',
                link: '/targets',
                iconClass: 'fa fa-bullseye',
                authorizations: [Authorization.ENVIRONMENT_ACCESS]
            },
            {
                label: 'menu.principal.envVariable',
                link: '/environmentsVariables',
                iconClass: 'fa fa-key',
                authorizations: [Authorization.ENVIRONMENT_ACCESS]
            },
            {
                label: 'menu.principal.plugins',
                link: '/plugins',
                iconClass: 'fa fa-cogs',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.roles',
                link: '/roles',
                iconClass: 'fa fa-user-shield',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.vacuum',
                link: '/vacuum',
                iconClass: 'fa fa-database',
                authorizations: [Authorization.ADMIN_ACCESS]
            },
            {
                label: 'menu.principal.workers',
                link: '/configurationAgent',
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
                link: '/metrics',
                iconClass: 'fa fa-chart-simple',
                authorizations: [Authorization.ADMIN_ACCESS]
            }
        ]
    }

];
