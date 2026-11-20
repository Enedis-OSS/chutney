/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.domain;

import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnfinishedExecutionsCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnfinishedExecutionsCleaner.class);

    private final ExecutionHistoryRepository executionHistoryRepository;
    private final TestCaseRepository testCaseRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignExecutionEngine campaignExecutionEngine;
    private final ChutneyMetrics metrics;

    public UnfinishedExecutionsCleaner(ExecutionHistoryRepository executionHistoryRepository,
                                       TestCaseRepository testCaseRepository,
                                       CampaignRepository campaignRepository,
                                       CampaignExecutionEngine campaignExecutionEngine,
                                       ChutneyMetrics metrics) {
        this.executionHistoryRepository = executionHistoryRepository;
        this.testCaseRepository = testCaseRepository;
        this.campaignRepository = campaignRepository;
        this.campaignExecutionEngine = campaignExecutionEngine;
        this.metrics = metrics;
    }

    /**
     * @return the number of executions set to KO
     */
    public int setUnfinishedExecutionsToKO() {
        List<ExecutionSummary> koExecutions = executionHistoryRepository.setAllRunningExecutionsToKO();

        koExecutions.forEach(this::sendScenarioExecutionMetrics);

        Set<Long> campaignExecutionsIds = new HashSet<>();
        koExecutions.stream()
            .map(ExecutionSummary::campaignReport)
            .flatMap(Optional::stream)
            .filter(campaignExecution -> campaignExecutionsIds.add(campaignExecution.executionId))
            .map(this::completeWithNotExecutedScenarios)
            .forEach(this::sendCampaignExecutionMetrics);

        return koExecutions.size();
    }

    private void sendScenarioExecutionMetrics(ExecutionSummary execution) {
        try {
            testCaseRepository.findById(execution.scenarioId()).ifPresent(testCase ->
                metrics.onScenarioExecutionEnded(testCase, executionHistoryRepository.getExecution(execution.scenarioId(), execution.executionId()))
            );
        } catch (Exception e) {
            LOGGER.error("Send metrics for execution {} failed", execution.executionId(), e);
        }
    }

    private CampaignExecution completeWithNotExecutedScenarios(CampaignExecution campaignExecution) {
        try {
            return campaignExecutionEngine.completeInterruptedExecution(campaignExecution.executionId);
        } catch (Exception e) {
            LOGGER.error("Complete interrupted campaign execution {} failed", campaignExecution.executionId, e);
            return campaignExecution;
        }
    }

    private void sendCampaignExecutionMetrics(CampaignExecution campaignExecution) {
        try {
            metrics.onCampaignExecutionEnded(campaignRepository.findById(campaignExecution.campaignId), campaignExecution);
        } catch (Exception e) {
            LOGGER.error("Send metrics for campaign execution {} failed", campaignExecution.executionId, e);
        }
    }
}
