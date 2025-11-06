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

    public void updateTestExecution(Long campaignId, Long campaignExecutionId, String scenarioId, String datasetId, ReportForJira report, String jiraId) {
        if (report != null && isNotEmpty(scenarioId) && campaignId != null) {
            jiraXrayService.updateTestExecution(campaignId, campaignExecutionId, scenarioId, datasetId, report, jiraId);
        }
    }

    public List<XrayTestExecTest> getTestStatusInTestExec(String testExec) { // TODO - Only used in a test ?
        return jiraXrayService.getTestExecutionScenarios(testExec);
    }
}


