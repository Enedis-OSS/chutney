/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.instrument;

import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import org.springframework.http.HttpStatusCode;

public interface ChutneyMetrics {

    void onScenarioExecutionEnded(TestCase testCase, ExecutionHistory.Execution execution);

    void onCampaignExecutionEnded(Campaign campaign, CampaignExecution campaignExecution);

    void onHttpError(HttpStatusCode status);

    void onJiraRestClientError(HttpStatusCode status);
}
