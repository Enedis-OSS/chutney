/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Execution } from '@core/model/scenario/execution.model';

export interface ExecutionSummaryDto {
    executionId: number;
    time: string;
    duration: number;
    status: string;
    environment: string;
    user: string;
    testCaseTitle: string;
    tags?: string[];
    info?: string;
    error?: string;
    scenarioId?: string;
}

export interface ScenarioIndexMetadataDto {
    id: string;
    title: string;
    description?: string;
    repositorySource?: string;
    tags: string[];
    executions: ExecutionSummaryDto[];
    creationDate: string;
    updateDate: string;
    author?: string;
    version?: number;
}

export interface TestCaseIndexDto {
    metadata: ScenarioIndexMetadataDto;
}

export class ScenarioIndex {

    public status;
    public lastExecution;

    constructor(
        public id?: string,
        public title?: string,
        public description?: string,
        public repositorySource?: string,
        public creationDate?: Date,
        public updateDate?: Date,
        public version?: number,
        public author?: string,
        public tags: Array<string> = [],
        public executions?: Array<Execution>,
        public jiraId?: Array<string>,
    ) {
        this.status = this.findStatus();
        this.lastExecution = this.lastTimeExec();
    }

    private findStatus() {
        if (this.executions && this.executions.length > 0) {
            return this.executions[0].status;
        } else {
            return 'NOT_EXECUTED';
        }
    }

    private lastTimeExec() {
        if (this.executions && this.executions.length > 0) {
            return this.executions[0].time;
        } else {
            return null;
        }
    }
}
