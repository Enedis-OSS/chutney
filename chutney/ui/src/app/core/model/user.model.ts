/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */


export class User {
  constructor(
    public id: string,
    public name?: string,
    public firstname?: string,
    public lastname?: string,
    public mail?: string,
    public authorizations?: Array<Authorization>,
  ) { }
}

export enum Authorization {
    SCENARIO_READ = 'SCENARIO_READ',
    SCENARIO_WRITE = 'SCENARIO_WRITE',

    CAMPAIGN_READ = 'CAMPAIGN_READ',
    CAMPAIGN_WRITE = 'CAMPAIGN_WRITE',

    EXECUTION_READ = 'EXECUTION_READ',
    EXECUTION_WRITE = 'EXECUTION_WRITE',

    ENVIRONMENT_READ = 'ENVIRONMENT_READ',
    ENVIRONMENT_WRITE = 'ENVIRONMENT_WRITE',

    TARGET_READ = 'TARGET_READ',
    TARGET_WRITE = 'TARGET_WRITE',

    VARIABLE_READ = 'VARIABLE_READ',
    VARIABLE_WRITE = 'VARIABLE_WRITE',

    DATASET_READ = 'DATASET_READ',
    DATASET_WRITE = 'DATASET_WRITE',

    ADMIN_ACCESS = 'ADMIN_ACCESS'
}
