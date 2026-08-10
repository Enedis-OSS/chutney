/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { fakeAsync, tick } from '@angular/core/testing';
import { defer, of, throwError } from 'rxjs';

import { Execution } from '@model';
import { ExecutionStatus } from '@core/model/scenario/execution-status';

import { ScenarioExecutionsHistoryComponent } from './scenario-executions-history.component';

describe('ScenarioExecutionsHistoryComponent', () => {
    it('should keep refreshing after a transient error and stop when no execution is running', fakeAsync(() => {
        const runningExecution = executionWithStatus(ExecutionStatus.RUNNING);
        const completedExecution = executionWithStatus(ExecutionStatus.SUCCESS);
        let requestCount = 0;
        const scenarioExecutionService = jasmine.createSpyObj('ScenarioExecutionService', ['findScenarioExecutions']);
        scenarioExecutionService.findScenarioExecutions.and.returnValue(defer(() =>
            ++requestCount === 1
                ? throwError(() => new Error('Temporary error'))
                : of([completedExecution])
        ));
        const component = new ScenarioExecutionsHistoryComponent(
            {} as any,
            {} as any,
            scenarioExecutionService,
            {} as any,
            {} as any,
            {} as any
        );
        component.executions = [runningExecution];

        (component as any).checkForRefresh();
        expect(requestCount).toBe(1);

        tick(5000);

        expect(requestCount).toBe(2);
        expect(component.executions).toEqual([completedExecution]);
        expect((component as any).isRefreshActive()).toBeFalse();

        tick(10000);
        expect(requestCount).toBe(2);
    }));
});

function executionWithStatus(status: ExecutionStatus): Execution {
    return new Execution(0, status, null, 1, new Date(), null, null, null, []);
}
