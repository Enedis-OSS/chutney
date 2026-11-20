/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { FormBuilder } from '@angular/forms';
import { of } from 'rxjs';
import { JiraScenario, ScenarioIndex } from '@model';
import { CampaignEditionComponent } from './campaign-edition.component';

describe('CampaignEditionComponent jira test execution', () => {

    const loginService = jasmine.createSpyObj('LoginService', ['hasAuthorization']);
    loginService.hasAuthorization.and.returnValue(true);
    const jiraLinkService = jasmine.createSpyObj('JiraPluginService', ['findTestExecScenarios']);

    function createComponent(jiraScenarios: JiraScenario[]): CampaignEditionComponent {
        jiraLinkService.findTestExecScenarios.and.returnValue(of(jiraScenarios));
        const component = new CampaignEditionComponent(
            null, null, jiraLinkService, null, new FormBuilder(), null, null, null, null, null, null, loginService, {}
        );
        component.scenarios = ['1', '2', '3'].map(id => ({id} as ScenarioIndex));
        component.campaignForm.controls['jiraId'].setValue('TEST-EXEC');
        component.campaignForm.controls['onlyLinkedScenarios'].setValue(true);
        return component;
    }

    it('should list each execution status once, even after a refresh', () => {
        const component = createComponent([
            new JiraScenario('J-1', '1', 'PASS'),
            new JiraScenario('J-2', '2', 'PASS'),
            new JiraScenario('J-3', '3', 'FAIL'),
            new JiraScenario('J-4', '3', ''),
        ]);

        component.refreshJiraScenarios();
        component.refreshJiraScenarios();

        expect(component.jiraItemList).toEqual([{id: 0, text: 'PASS'}, {id: 1, text: 'FAIL'}]);
    });

    it('should keep scenarios whose dataset jira link has the selected execution status', () => {
        const component = createComponent([
            new JiraScenario('J-1', '1', 'PASS'),
            new JiraScenario('J-2', '2', 'PASS'),
            new JiraScenario('J-2-DATASET', '2', 'FAIL'),
        ]);
        component.refreshJiraScenarios();

        component.onJiraItemSelect({id: 1, text: 'FAIL'});

        expect(component.jiraScenariosToExclude.map(s => s.id)).toEqual(['1', '3']);
    });
});
