/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.domain;

import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.FAILURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.campaign.domain.CampaignNotFoundException;
import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecutionReportBuilder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnfinishedExecutionsCleanerTest {

    private final ExecutionHistoryRepository executionHistoryRepository = mock(ExecutionHistoryRepository.class);
    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
    private final CampaignRepository campaignRepository = mock(CampaignRepository.class);
    private final CampaignExecutionEngine campaignExecutionEngine = mock(CampaignExecutionEngine.class);
    private final ChutneyMetrics metrics = mock(ChutneyMetrics.class);

    private final UnfinishedExecutionsCleaner sut = new UnfinishedExecutionsCleaner(executionHistoryRepository, testCaseRepository, campaignRepository, campaignExecutionEngine, metrics);

    @Test
    void send_scenario_metrics_for_each_execution_set_to_KO() {
        TestCase testCase = givenTestCase("1");
        ExecutionHistory.Execution koExecution = givenKoExecution("1", 10L, null);

        int koExecutionsCount = sut.setUnfinishedExecutionsToKO();

        assertThat(koExecutionsCount).isEqualTo(1);
        verify(metrics).onScenarioExecutionEnded(testCase, koExecution);
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void send_campaign_metrics_once_by_campaign_execution_having_executions_set_to_KO() {
        Campaign campaign = mock(Campaign.class);
        when(campaignRepository.findById(5L)).thenReturn(campaign);
        CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder().executionId(50L).campaignId(5L).userId("").build();
        when(campaignExecutionEngine.completeInterruptedExecution(50L)).thenReturn(campaignExecution);
        givenTestCase("1");
        givenTestCase("2");
        ExecutionSummary first = givenKoExecution("1", 10L, campaignExecution).summary();
        ExecutionSummary second = givenKoExecution("2", 20L, campaignExecution).summary();
        when(executionHistoryRepository.setAllRunningExecutionsToKO()).thenReturn(List.of(first, second));

        sut.setUnfinishedExecutionsToKO();

        verify(metrics).onCampaignExecutionEnded(campaign, campaignExecution);
    }

    @Test
    void complete_interrupted_campaign_execution_with_not_executed_scenarios_before_sending_its_metrics() {
        Campaign campaign = mock(Campaign.class);
        when(campaignRepository.findById(5L)).thenReturn(campaign);
        CampaignExecution interruptedExecution = CampaignExecutionReportBuilder.builder().executionId(50L).campaignId(5L).userId("").build();
        CampaignExecution completedExecution = CampaignExecutionReportBuilder.builder().executionId(50L).campaignId(5L).userId("").build();
        when(campaignExecutionEngine.completeInterruptedExecution(50L)).thenReturn(completedExecution);
        givenTestCase("1");
        givenKoExecution("1", 10L, interruptedExecution);

        sut.setUnfinishedExecutionsToKO();

        ArgumentCaptor<CampaignExecution> sentExecution = ArgumentCaptor.forClass(CampaignExecution.class);
        verify(metrics).onCampaignExecutionEnded(eq(campaign), sentExecution.capture());
        assertThat(sentExecution.getValue()).isSameAs(completedExecution);
    }

    @Test
    void do_not_fail_when_scenario_or_campaign_no_longer_exists() {
        when(testCaseRepository.findById("1")).thenReturn(Optional.empty());
        when(campaignRepository.findById(5L)).thenThrow(new CampaignNotFoundException(5L));
        when(campaignExecutionEngine.completeInterruptedExecution(50L)).thenThrow(new CampaignNotFoundException(5L));
        givenKoExecution("1", 10L, CampaignExecutionReportBuilder.builder().executionId(50L).campaignId(5L).userId("").build());

        int koExecutionsCount = sut.setUnfinishedExecutionsToKO();

        assertThat(koExecutionsCount).isEqualTo(1);
        verify(metrics, never()).onScenarioExecutionEnded(any(), any());
        verify(metrics, never()).onCampaignExecutionEnded(any(), any());
    }

    private TestCase givenTestCase(String scenarioId) {
        TestCase testCase = mock(TestCase.class);
        when(testCaseRepository.findById(scenarioId)).thenReturn(Optional.of(testCase));
        return testCase;
    }

    private ExecutionHistory.Execution givenKoExecution(String scenarioId, Long executionId, CampaignExecution campaignExecution) {
        ExecutionHistory.Execution koExecution = ImmutableExecutionHistory.Execution.builder()
            .executionId(executionId)
            .scenarioId(scenarioId)
            .time(LocalDateTime.now())
            .duration(3L)
            .status(FAILURE)
            .report("")
            .testCaseTitle("")
            .environment("")
            .user("")
            .campaignReport(Optional.ofNullable(campaignExecution))
            .build();
        when(executionHistoryRepository.getExecution(scenarioId, executionId)).thenReturn(koExecution);
        when(executionHistoryRepository.setAllRunningExecutionsToKO()).thenReturn(List.of(koExecution.summary()));
        return koExecution;
    }
}
