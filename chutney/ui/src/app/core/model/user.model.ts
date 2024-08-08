/**
 * Copyright 2017-2024 Enedis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    SCENARIO_EXECUTE = 'SCENARIO_EXECUTE',

    CAMPAIGN_READ = 'CAMPAIGN_READ',
    CAMPAIGN_WRITE = 'CAMPAIGN_WRITE',
    CAMPAIGN_EXECUTE = 'CAMPAIGN_EXECUTE',

    ENVIRONMENT_ACCESS = 'ENVIRONMENT_ACCESS',

    GLOBAL_VAR_READ = 'GLOBAL_VAR_READ',
    GLOBAL_VAR_WRITE = 'GLOBAL_VAR_WRITE',

    DATASET_READ = 'DATASET_READ',
    DATASET_WRITE = 'DATASET_WRITE',

    ADMIN_ACCESS = 'ADMIN_ACCESS'
}
