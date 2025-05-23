/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.domain;

import fr.enedis.chutney.jira.xrayapi.Xray;
import fr.enedis.chutney.jira.xrayapi.XrayTestExecTest;
import java.util.List;

public interface JiraXrayApi {

    void updateRequest(Xray xray);

    List<XrayTestExecTest> getTestExecutionScenarios(String xrayId);

    void updateStatusByTestRunId(String id, String executionStatus);

    void associateTestExecutionFromTestPlan(String testPlanId, String testExecutionId);

    String createTestExecution(String testPlanId);

    boolean isTestPlan(String issueId);

}
