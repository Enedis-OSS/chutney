/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import fr.enedis.chutney.jira.domain.JiraXrayService;
import fr.enedis.chutney.jira.xrayapi.XrayTestExecTest;
import java.util.List;

public class JiraXrayEmbeddedApi {

    private final JiraXrayService jiraXrayService;

    public JiraXrayEmbeddedApi(JiraXrayService jiraXrayService) {
        this.jiraXrayService = jiraXrayService;
    }

    public void updateTestExecution(ScenarioJiraLink scenarioJiraLink, ReportForJira report) {
        if (report != null && isNotEmpty(scenarioJiraLink.scenarioId()) && scenarioJiraLink.campaignId() != null) {
            jiraXrayService.updateTestExecution(scenarioJiraLink, report);
        }
    }

    public List<XrayTestExecTest> getTestStatusInTestExec(String testExec) { // TODO - Only used in a test ?
        return jiraXrayService.getTestExecutionScenarios(testExec);
    }
}


