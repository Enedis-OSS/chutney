/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { of } from 'rxjs';
import { CampaignReport, Dataset, JiraScenario, JiraTestExecutionScenarios, ScenarioExecutionReportOutline } from '@model';
import { ExecutionDataset } from '@core/model/scenario/execution.dataset';
import { CampaignExecutionComponent } from './campaign-execution.component';

describe('CampaignExecutionComponent jira links', () => {

    const scenarioLink = new JiraScenario('J-SCENARIO', '1', 'PASS');
    const datasetLink = new JiraScenario('J-DATASET', '1', 'FAIL', 'dataset_1');
    const datasetOnlyLink = new JiraScenario('J-2-DATASET', '2', 'PASS', 'dataset_1');

    const withoutDataset = new ScenarioExecutionReportOutline('1', 11);
    const withLinkedDataset = execution('1', 12, 'dataset_1');
    const withOtherDataset = execution('1', 13, 'dataset_2');
    const datasetOnlyWithoutDataset = new ScenarioExecutionReportOutline('2', 14);
    const withFetchedDataset = new ScenarioExecutionReportOutline('2', 15);

    const jiraLinkService = jasmine.createSpyObj('JiraPluginService', ['findTestExecScenariosByCampaignExecution', 'updateScenarioStatus']);
    const scenarioExecutionService = jasmine.createSpyObj('ScenarioExecutionService', ['findExecutionReport']);

    function execution(scenarioId: string, executionId: number, datasetId: string): ScenarioExecutionReportOutline {
        const outline = new ScenarioExecutionReportOutline(scenarioId, executionId);
        outline.dataset = {id: datasetId} as Dataset;
        return outline;
    }

    function createComponent(): CampaignExecutionComponent {
        jiraLinkService.findTestExecScenariosByCampaignExecution.and.returnValue(
            of(new JiraTestExecutionScenarios('TEST-EXEC', [scenarioLink, datasetLink, datasetOnlyLink]))
        );
        jiraLinkService.updateScenarioStatus.calls.reset();
        jiraLinkService.updateScenarioStatus.and.returnValue(of({}));
        scenarioExecutionService.findExecutionReport.and.callFake((_scenarioId: string, executionId: number) =>
            of({dataset: executionId === 15 ? new ExecutionDataset([], [], 'dataset_1') : undefined})
        );

        const component = new CampaignExecutionComponent(jiraLinkService, null, null, null, scenarioExecutionService, null);
        component.jiraUrl = 'http://jira';
        component.report = new CampaignReport({
            executionId: 1,
            scenarioExecutionReports: [withoutDataset, withLinkedDataset, withOtherDataset, datasetOnlyWithoutDataset, withFetchedDataset],
            user: '',
            jiraId: ''
        });
        component.ngOnInit();
        return component;
    }

    it('should show the jira link and status matching the execution dataset', () => {
        const component = createComponent();

        expect(component.jiraLinkFrom(withoutDataset)).toBe('http://jira/browse/J-SCENARIO');
        expect(component.scenarioStatus(withoutDataset)).toBe('PASS');
        expect(component.jiraLinkFrom(withLinkedDataset)).toBe('http://jira/browse/J-DATASET');
        expect(component.scenarioStatus(withLinkedDataset)).toBe('FAIL');
        expect(component.jiraLinkFrom(withOtherDataset)).toBe('http://jira/browse/J-SCENARIO');
        expect(component.jiraLinkFrom(withFetchedDataset)).toBe('http://jira/browse/J-2-DATASET');
    });

    it('should show no jira link when only another dataset is linked', () => {
        const component = createComponent();

        expect(component.jiraLinkFrom(datasetOnlyWithoutDataset)).toBeNull();
        expect(component.scenarioStatus(datasetOnlyWithoutDataset)).toBe(component.UNSUPPORTED);
    });

    it('should update the status of the jira test matching the execution dataset only', () => {
        const component = createComponent();

        component.selectedUpdateStatus(withLinkedDataset, {target: {value: 'PASS'}});

        expect(component.isStatusUpdatable(withLinkedDataset)).toBeTrue();
        expect(component.isStatusUpdatable(withoutDataset)).toBeFalse();

        component.updateStatus(withoutDataset);
        expect(jiraLinkService.updateScenarioStatus).not.toHaveBeenCalled();

        component.updateStatus(withLinkedDataset);
        expect(jiraLinkService.updateScenarioStatus).toHaveBeenCalledOnceWith('TEST-EXEC', datasetLink, 'PASS');
    });
});
