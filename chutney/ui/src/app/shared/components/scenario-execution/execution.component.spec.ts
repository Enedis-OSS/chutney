/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { fakeAsync, tick } from '@angular/core/testing';

import { ExecutionStatus } from '@core/model/scenario/execution-status';
import { StepExecutionReport } from '@model';

import { ScenarioExecutionComponent } from './execution.component';

describe('ScenarioExecutionComponent', () => {
    let component: ScenarioExecutionComponent;

    beforeEach(() => {
        component = new ScenarioExecutionComponent(
            null, null, null, null, null, null, null
        );
    });

    it('should automatically select the first failed step on initial report display', fakeAsync(() => {
        const failedStep = step('failed', ExecutionStatus.FAILURE);
        setReport([failedStep]);
        spyOn(component, 'selectStep');

        component['afterReportUpdate']();
        tick(500);

        expect(component.selectStep).toHaveBeenCalledOnceWith(failedStep, true);
    }));

    it('should preserve a manually selected sub-step during retry refresh', fakeAsync(() => {
        const selectedSubStep = step('selected', ExecutionStatus.SUCCESS);
        const retryStep = step('retry', ExecutionStatus.FAILURE, [selectedSubStep]);
        setReport([retryStep]);
        component.selectStep(selectedSubStep);
        spyOn(component, 'selectStep').and.callThrough();

        component['afterReportUpdate']();
        tick(500);

        expect(component.selectedStep).toBe(selectedSubStep);
        expect(component.selectStep).not.toHaveBeenCalled();
    }));

    it('should not override a selection made while failed-step selection is pending', fakeAsync(() => {
        const failedStep = step('failed', ExecutionStatus.FAILURE);
        const manuallySelectedStep = step('manually selected', ExecutionStatus.SUCCESS);
        setReport([failedStep, manuallySelectedStep]);

        component['afterReportUpdate']();
        component.selectStep(manuallySelectedStep);
        tick(500);

        expect(component.selectedStep).toBe(manuallySelectedStep);
    }));

    it('should preserve an explicit root selection during retry refresh', fakeAsync(() => {
        setReport([step('retry', ExecutionStatus.FAILURE)]);
        component.selectStep();
        spyOn(component, 'selectStep').and.callThrough();

        component['afterReportUpdate']();
        tick(500);

        expect(component.selectedStep).toBeNull();
        expect(component.selectStep).not.toHaveBeenCalled();
    }));

    it('should restore the selected step from a replaced report tree', () => {
        const previousSelectedStep = step('selected before refresh', ExecutionStatus.RUNNING);
        previousSelectedStep['rowId'] = '0-0';
        component.selectedStep = previousSelectedStep;
        const refreshedSelectedStep = step('selected after refresh', ExecutionStatus.SUCCESS);
        setReport([step('retry', ExecutionStatus.RUNNING, [refreshedSelectedStep])]);

        component['afterReportUpdate']();

        expect(component.selectedStep).toBe(refreshedSelectedStep);
    });

    function setReport(steps: StepExecutionReport[]) {
        component.scenarioExecutionReport = {
            contextVariables: {},
            report: { steps }
        } as any;
    }

    function step(name: string, status: ExecutionStatus, steps: StepExecutionReport[] = []): StepExecutionReport {
        return { name, status, steps } as StepExecutionReport;
    }
});
