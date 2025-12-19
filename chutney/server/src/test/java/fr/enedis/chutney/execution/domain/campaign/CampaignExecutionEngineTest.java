/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.domain.campaign;

import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.FAILURE;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.NOT_EXECUTED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.SUCCESS;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static util.WaitUtils.awaitDuring;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.domain.CampaignNotFoundException;
import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.dataset.domain.DataSetRepository;
import fr.enedis.chutney.jira.api.JiraXrayEmbeddedApi;
import fr.enedis.chutney.jira.api.ReportForJira;
import fr.enedis.chutney.jira.api.ExecutionJiraLink;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.ExecutionRequest;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngine;
import fr.enedis.chutney.server.core.domain.execution.ScenarioExecutionEngineAsync;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecutionReportBuilder;
import fr.enedis.chutney.server.core.domain.scenario.campaign.ScenarioExecutionCampaign;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StopWatch;

public class CampaignExecutionEngineTest {

    private static ExecutorService executorService;

    private CampaignExecutionEngine sut;

    private final CampaignRepository campaignRepository = mock(CampaignRepository.class);
    private final CampaignExecutionRepository campaignExecutionRepository = mock(CampaignExecutionRepository.class);
    private final ScenarioExecutionEngine scenarioExecutionEngine = mock(ScenarioExecutionEngine.class);
    private final ScenarioExecutionEngineAsync scenarioExecutionEngineAsync = mock(ScenarioExecutionEngineAsync.class);
    private final ExecutionHistoryRepository executionHistoryRepository = mock(ExecutionHistoryRepository.class);
    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
    private final JiraXrayEmbeddedApi jiraXrayPlugin = mock(JiraXrayEmbeddedApi.class);
    private final ChutneyMetrics metrics = mock(ChutneyMetrics.class);
    private final DataSetRepository datasetRepository = mock(DataSetRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();


    private GwtTestCase firstTestCase;
    private GwtTestCase secondTestCase;
    long firstScenarioExecutionId = 10L;
    long secondScenarioExecutionId = 20L;

    @BeforeAll
    public static void setUpAll() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(2);
        taskExecutor.initialize();
        executorService = new ExecutorServiceAdapter(taskExecutor);
    }

    @BeforeEach
    public void setUp() {
        sut = new CampaignExecutionEngine(campaignRepository, campaignExecutionRepository, scenarioExecutionEngine, scenarioExecutionEngineAsync, executionHistoryRepository, testCaseRepository, jiraXrayPlugin, metrics, executorService, datasetRepository, objectMapper);
        firstTestCase = createAndMockExecutedGwtTestCase(firstScenarioExecutionId, SUCCESS);
        secondTestCase = createAndMockExecutedGwtTestCase(secondScenarioExecutionId, SUCCESS);

        when(scenarioExecutionEngine.execute(any(ExecutionRequest.class))).thenReturn(mock(ScenarioExecutionReport.class));
        when(scenarioExecutionEngine.saveNotExecutedScenarioExecution(any(ExecutionRequest.class))).thenReturn(mock(ExecutionHistory.Execution.class));
    }

    @Test
    public void update_jira_xray_for_completed_execution() {
        // Given
        GwtTestCase notExecutedTestCase = createAndMockExecutedGwtTestCase(30L, NOT_EXECUTED);
        Campaign campaign = createCampaign(List.of(firstTestCase, notExecutedTestCase));

        // When
        CampaignExecution cer = sut.executeScenarioInCampaign(campaign, "user", null, "JIRA-100");

        ArgumentCaptor<ReportForJira> reportForJiraCaptor = ArgumentCaptor.forClass(ReportForJira.class);
        verify(jiraXrayPlugin).updateTestExecution(eq(new ExecutionJiraLink(campaign.id, cer.executionId, firstTestCase.metadata.id, "", "JIRA-100")), reportForJiraCaptor.capture());
        verify(jiraXrayPlugin, times(0)).updateTestExecution(eq(new ExecutionJiraLink(campaign.id, cer.executionId, notExecutedTestCase.metadata.id, "", "JIRA-100")), reportForJiraCaptor.capture());
    }

    @Test
    public void execute_scenarios_in_sequence_and_store_reports_in_campaign_report_when_executed() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));

        // When
        CampaignExecution campaignExecution = sut.executeScenarioInCampaign(campaign, "user", null);

        // Then
        verify(testCaseRepository, times(2)).findExecutableById(anyString());
        verify(scenarioExecutionEngine, times(2)).execute(any(ExecutionRequest.class));
        verify(executionHistoryRepository, times(4)).getExecution(anyString(), anyLong());

        assertThat(campaignExecution.scenarioExecutionReports()).hasSize(campaign.scenarios.size());
        assertThat(campaignExecution.scenarioExecutionReports().getFirst().execution().executionId()).isEqualTo(firstScenarioExecutionId);
        assertThat(campaignExecution.scenarioExecutionReports().get(1).execution().executionId()).isEqualTo(secondScenarioExecutionId);
        assertThat(campaignExecution.partialExecution).isFalse();
        verify(campaignExecutionRepository).saveCampaignExecution(campaign.id, campaignExecution);
        verify(metrics).onCampaignExecutionEnded(
            eq(campaign),
            eq(campaignExecution)
        );
    }

    @Test
    public void execute_partially_scenarios_requested() {
        // Given
        Campaign campaign = createCampaign(List.of(createGwtTestCase("not executed test case"), secondTestCase));

        // When
        CampaignExecution campaignExecution = sut.executeScenarioInCampaign(singletonList(
            new ScenarioExecutionCampaign(
                String.valueOf(secondScenarioExecutionId),
                secondTestCase.metadata.title,
                createExecution(String.valueOf(secondScenarioExecutionId), secondScenarioExecutionId).summary())
        ), campaign, "user", null, null);

        // Then
        verify(testCaseRepository).findExecutableById(anyString());
        verify(scenarioExecutionEngine).execute(any(ExecutionRequest.class));
        verify(executionHistoryRepository, times(2)).getExecution(anyString(), anyLong());

        assertThat(campaignExecution.scenarioExecutionReports()).hasSize(1);
        assertThat(campaignExecution.scenarioExecutionReports().getFirst().execution().executionId()).isEqualTo(secondScenarioExecutionId);
        assertThat(campaignExecution.partialExecution).isTrue();
        verify(campaignExecutionRepository).saveCampaignExecution(campaign.id, campaignExecution);
    }

    @Test
    public void stop_execution_of_scenarios_when_requested() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));

        when(scenarioExecutionEngine.execute(any(ExecutionRequest.class))).then((Answer<ScenarioExecutionReport>) invocationOnMock -> {
            awaitDuring(1, SECONDS);
            return mock(ScenarioExecutionReport.class);
        });

        var firstScenarioExecution = createExecution(firstTestCase.id(), firstScenarioExecutionId);
        when(executionHistoryRepository.getExecution(eq(firstTestCase.id()), or(eq(0L), eq(firstScenarioExecutionId))))
            .thenReturn(firstScenarioExecution);

        when(scenarioExecutionEngine.saveNotExecutedScenarioExecution(any(ExecutionRequest.class)))
            .thenReturn(createExecution(secondTestCase.id(), secondScenarioExecutionId, NOT_EXECUTED));

        CampaignExecution campaignExecution = mock(CampaignExecution.class);
        when(campaignExecution.scenarioExecutionReports())
            .thenReturn(List.of(
                new ScenarioExecutionCampaign(firstTestCase.id(), firstScenarioExecution.testCaseTitle(), createExecution(firstTestCase.id(), firstScenarioExecutionId, ServerReportStatus.RUNNING).summary())
            ));
        when(campaignExecutionRepository.getCampaignExecutionById(anyLong()))
            .thenReturn(campaignExecution);

        // When
        AtomicReference<CampaignExecution> campaignExecutionReport = new AtomicReference<>();

        Executors.newFixedThreadPool(1).submit(() -> campaignExecutionReport.set(sut.executeScenarioInCampaign(campaign, "user")));

        awaitDuring(500, MILLISECONDS);
        sut.stopExecution(0L);
        awaitDuring(1, SECONDS);

        // Then
        verify(scenarioExecutionEngine).execute(any(ExecutionRequest.class));
        verify(executionHistoryRepository, times(3)).getExecution(anyString(), anyLong());
        verify(campaignExecutionRepository).getCampaignExecutionById(0L);
        verify(scenarioExecutionEngineAsync).stop(firstTestCase.id(), firstScenarioExecutionId);

        assertThat(campaignExecutionReport.get().status()).isEqualTo(ServerReportStatus.STOPPED);
        assertThat(campaignExecutionReport.get().scenarioExecutionReports()).hasSize(2);
        assertThat(campaignExecutionReport.get().scenarioExecutionReports().getFirst().status()).isEqualTo(SUCCESS);
        assertThat(campaignExecutionReport.get().scenarioExecutionReports().get(1).status()).isEqualTo(NOT_EXECUTED);
        assertThat(campaignExecutionReport.get().scenarioExecutionReports()).hasSize(2);
        assertThat(campaignExecutionReport.get().scenarioExecutionReports().getFirst().execution().executionId()).isEqualTo(firstScenarioExecutionId);
        assertThat(campaignExecutionReport.get().scenarioExecutionReports().get(1).execution().executionId()).isEqualTo(secondScenarioExecutionId);
    }

    @Test
    public void retry_failed_scenario() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase), true);

        when(executionHistoryRepository.getExecution(eq(firstTestCase.id()), or(eq(0L), eq(firstScenarioExecutionId)))).thenReturn(failedExecutionWithId(firstTestCase.id(), firstScenarioExecutionId));
        when(executionHistoryRepository.getExecution(eq(secondTestCase.id()), or(eq(0L), eq(secondScenarioExecutionId)))).thenReturn(failedExecutionWithId(secondTestCase.id(), secondScenarioExecutionId));

        // When
        sut.executeScenarioInCampaign(campaign, "user");

        // Then
        verify(scenarioExecutionEngine, times(4)).execute(any(ExecutionRequest.class));
    }

    @Test
    public void execute_scenario_in_parallel() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase), true, false);

        when(scenarioExecutionEngine.execute(any(ExecutionRequest.class))).then((Answer<ScenarioExecutionReport>) invocationOnMock -> {
            awaitDuring(1, SECONDS);
            return mock(ScenarioExecutionReport.class);
        });
        when(executionHistoryRepository.getExecution(eq(firstTestCase.id()), or(eq(0L), eq(firstScenarioExecutionId)))).thenReturn(failedExecutionWithId(firstTestCase.id(), firstScenarioExecutionId));
        when(executionHistoryRepository.getExecution(eq(secondTestCase.id()), or(eq(0L), eq(secondScenarioExecutionId)))).thenReturn(failedExecutionWithId(secondTestCase.id(), secondScenarioExecutionId));

        // When
        StopWatch watch = new StopWatch();
        watch.start();
        sut.executeScenarioInCampaign(campaign, "user");
        watch.stop();

        // Then
        verify(scenarioExecutionEngine, times(2)).execute(any(ExecutionRequest.class));
        assertThat(watch.getTotalTimeSeconds()).isLessThan(1.9);
    }

    @Test
    public void throw_when_no_campaign_found_on_execute_by_id() {
        when(campaignRepository.findById(anyLong())).thenReturn(null);
        assertThatThrownBy(() -> sut.executeById(generateId(), ""))
            .isInstanceOf(CampaignNotFoundException.class);
    }

    @Test
    public void throw_when_campaign_already_running_on_the_same_env() {
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));

        CampaignExecution mockReport = CampaignExecutionReportBuilder.builder()
            .environment(campaign.executionEnvironment())
            .userId("")
            .status(ServerReportStatus.RUNNING)
            .build();
        when(campaignExecutionRepository.currentExecutions(campaign.id)).thenReturn(List.of(mockReport));

        // When
        assertThatThrownBy(() -> sut.executeScenarioInCampaign(campaign, "user"))
            .isInstanceOf(CampaignAlreadyRunningException.class);
    }

    @Test
    public void throw_when_campaign_is_empty() {
        Campaign campaign = createCampaign();

        // When
        assertThatThrownBy(() -> sut.executeScenarioInCampaign(campaign, "user"))
            .isInstanceOf(CampaignEmptyExecutionException.class);
    }

    @Test
    public void throw_when_replay_is_empty() {
        when(campaignExecutionRepository.getCampaignExecutionById(1L)).thenReturn(
            CampaignExecutionReportBuilder.builder()
                .addScenarioExecutionReport(
                    new ScenarioExecutionCampaign("1", "", createExecution("1", 1L).summary())
                )
                .build()
        );

        assertThatThrownBy(() -> sut.replayFailedScenariosExecutionsForExecution(1L, ""))
            .isInstanceOf(CampaignEmptyExecutionException.class);
    }

    @Test
    public void execute_campaign_in_parallel_on_two_different_envs() {
        String otherEnv = "otherEnv";
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));

        CampaignExecution mockReport = CampaignExecutionReportBuilder.builder()
            .environment(otherEnv)
            .userId("")
            .build();
        when(campaignExecutionRepository.currentExecutions(anyLong())).thenReturn(List.of(mockReport));

        // When
        assertDoesNotThrow(() -> sut.executeScenarioInCampaign(campaign, "user"));
    }

    @Test
    public void generate_campaign_execution_id_when_executed() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));

        when(campaignRepository.findByName(campaign.title)).thenReturn(singletonList(campaign));
        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);

        // When
        sut.executeById(campaign.id, "");
        sut.executeByName(campaign.title, null, "");

        // Then
        verify(campaignRepository).findById(campaign.id);
        verify(campaignRepository).findByName(campaign.title);

        verify(campaignExecutionRepository, times(2)).generateCampaignExecutionId(campaign.id, campaign.executionEnvironment(), null);
    }

    @Test
    public void generate_campaign_execution_id_when_executed_with_dataset() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));

        when(campaignRepository.findByName(campaign.title)).thenReturn(singletonList(campaign));
        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);

        // When
        DataSet dataset = DataSet.builder().withId("datasetId").withName("").build();
        sut.executeById(campaign.id, "", dataset, "");
        sut.executeByName(campaign.title, null, dataset, "");

        // Then
        verify(campaignRepository).findById(campaign.id);
        verify(campaignRepository).findByName(campaign.title);

        verify(campaignExecutionRepository, times(2)).generateCampaignExecutionId(campaign.id, campaign.executionEnvironment(), dataset);
    }

    @Test
    public void return_last_existing_campaign_execution_for_existing_campaign() {
        // Given
        Campaign campaign = createCampaign();
        CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
            .executionId(123L)
            .campaignId(campaign.id)
            .environment(campaign.executionEnvironment())
            .build();

        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);
        when(campaignExecutionRepository.getLastExecution(campaign.id)).thenReturn(campaignExecution);

        // When
        CampaignExecution result = sut.getLastCampaignExecution(campaign.id);

        // Then
        verify(campaignRepository).findById(campaign.id);
        verify(campaignExecutionRepository).getLastExecution(campaign.id);

        assertThat(result).isEqualTo(campaignExecution);
    }

    @Test
    public void throw_exception_when_campaign_does_not_exists() {
        // Given
        Campaign campaign = createCampaign();

        when(campaignRepository.findById(campaign.id)).thenReturn(null);

        // When
        assertThatThrownBy(() -> sut.getLastCampaignExecution(campaign.id));

        // Then
        verify(campaignRepository).findById(campaign.id);
    }

    @Test
    public void execute_campaign_with_given_environment_when_executed_by_id() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));
        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);

        // When
        String executionEnv = "executionEnv";
        String executionUser = "executionUser";
        sut.executeById(campaign.id, executionEnv, null, executionUser);

        // Then
        verify(campaignRepository).findById(campaign.id);
        assertThat(campaign.executionEnvironment()).isEqualTo(executionEnv);
    }

    @Test
    public void execute_campaign_with_given_dataset_and_jira_id_when_executed_by_id() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));
        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);

        // When
        DataSet executionDataset = DataSet.builder().withName("executionDataset").withId("executionDataset").build();
        String executionUser = "executionUser";
        CampaignExecution execution = sut.executeById(campaign.id, null, executionDataset, executionUser, "JIRA-6");

        // Then
        verify(campaignRepository).findById(campaign.id);
        assertThat(execution.dataset.id).isEqualTo(executionDataset.id);
        assertThat(execution.jiraId).isEqualTo("JIRA-6");
        verify(jiraXrayPlugin).linkCampaignExecution(execution.executionId, "JIRA-6");
    }

    @Test
    public void execute_campaign_with_given_environment_when_executed_by_name() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));
        when(campaignRepository.findByName(anyString())).thenReturn(singletonList(campaign));

        // When
        String executionEnv = "executionEnv";
        String executionUser = "executionUser";
        sut.executeByName(campaign.title, executionEnv, null, executionUser);

        // Then
        verify(campaignRepository).findByName(campaign.title);
        assertThat(campaign.executionEnvironment()).isEqualTo(executionEnv);
    }

    @Test
    public void execute_campaign_with_given_dataset_when_executed_by_name() {
        // Given
        Campaign campaign = createCampaign(List.of(firstTestCase, secondTestCase));
        when(campaignRepository.findByName(anyString())).thenReturn(singletonList(campaign));

        // When
        DataSet executionDataset = DataSet.builder().withName("executionDataset").withId("executionDataset").build();
        String executionUser = "executionUser";
        List<CampaignExecution> campaignExecutions = sut.executeByName(campaign.title, null, executionDataset, executionUser);

        // Then
        verify(campaignRepository).findByName(campaign.title);
        assertThat(campaignExecutions).hasSize(1);
        assertThat(campaignExecutions.getFirst().dataset.id).isEqualTo(executionDataset.id);
    }

    @Test
    public void retrieve_current_campaign_execution_on_a_given_env() {
        String env = "env";
        CampaignExecution report = CampaignExecutionReportBuilder.builder()
            .executionId(1L)
            .campaignId(33L)
            .environment(env)
            .build();
        String otherEnv = "otherEnv";
        CampaignExecution report2 = CampaignExecutionReportBuilder.builder()
            .executionId(2L)
            .campaignId(33L)
            .environment(otherEnv)
            .build();
        CampaignExecution report3 = CampaignExecutionReportBuilder.builder()
            .executionId(3L)
            .campaignId(42L)
            .environment(otherEnv)
            .build();
        when(campaignExecutionRepository.currentExecutions(33L)).thenReturn(List.of(report, report2));
        when(campaignExecutionRepository.currentExecutions(42L)).thenReturn(List.of(report3));

        Optional<CampaignExecution> campaignExecutionReport = sut.currentExecution(33L, env);

        assertThat(campaignExecutionReport).isNotEmpty();
        assertThat(campaignExecutionReport.get().executionId).isEqualTo(1L);
        assertThat(campaignExecutionReport.get().executionEnvironment).isEqualTo(env);
    }

    @Test
    public void throw_when_stop_unknown_campaign_execution() {
        assertThatThrownBy(() -> sut.stopExecution(generateId()))
            .isInstanceOf(CampaignExecutionNotFoundException.class);
    }

    @Test
    public void throw_when_execute_unknown_campaign_execution() {
        assertThatThrownBy(() -> sut.executeById(generateId(), ""))
            .isInstanceOf(CampaignNotFoundException.class);
    }

    @Test
    public void use_campaign_default_dataset_before_execution_when_scenario_in_campaign_does_not_define_dataset() {
        // Given
        TestCase gwtTestCase = GwtTestCase.builder()
            .withMetadata(
                TestCaseMetadataImpl.builder()
                    .withId("gwt")
                    .build()
            )
            .build();

        var campaignScenarios = singletonList(new Campaign.CampaignScenario(gwtTestCase.id(), null));
        Campaign campaign = new Campaign(generateId(), "...", null, campaignScenarios, "...", false, false, "campaignDataSet", List.of("TAG"));

        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);
        when(testCaseRepository.findExecutableById(gwtTestCase.id())).thenReturn(of(gwtTestCase));
        when(executionHistoryRepository.getExecution(any(), any())).thenReturn(createExecution(gwtTestCase.id(), 42L));

        when(datasetRepository.findById(eq("campaignDataSet"))).thenReturn(DataSet.builder().withName("campaignDataSet").build());

        // When
        sut.executeById(campaign.id, "user");

        // Then
        ArgumentCaptor<ExecutionRequest> argumentCaptor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(scenarioExecutionEngine).execute(argumentCaptor.capture());
        ExecutionRequest executionRequest = argumentCaptor.getValue();
        assertThat(executionRequest.dataset).isNotNull();
        assertThat(executionRequest.dataset.name).isEqualTo("campaignDataSet");
        assertThat(executionRequest.tags).containsExactly("TAG");
    }

    @Test
    public void use_scenario_dataset_over_campaign_default_dataset_before_execution_when_scenario_in_campaign_defines_dataset() {
        // Given
        TestCase gwtTestCase = GwtTestCase.builder()
            .withMetadata(
                TestCaseMetadataImpl.builder()
                    .withId("gwt")
                    .withDefaultDataset("scenarioDefaultDataset")
                    .build()
            )
            .build();

        var campaignScenarios = singletonList(new Campaign.CampaignScenario(gwtTestCase.id(), "scenarioInCampaignDataset"));
        Campaign campaign = new Campaign(generateId(), "...", null, campaignScenarios, "...", false, false, "campaignDataSet", List.of("TAG"));

        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);
        when(testCaseRepository.findExecutableById(gwtTestCase.id())).thenReturn(of(gwtTestCase));
        when(executionHistoryRepository.getExecution(any(), any())).thenReturn(createExecution(gwtTestCase.id(), 42L));

        when(datasetRepository.findById(eq("scenarioInCampaignDataset"))).thenReturn(DataSet.builder().withName("scenarioInCampaignDataset").build());

        // When
        sut.executeById(campaign.id, "user");

        // Then
        ArgumentCaptor<ExecutionRequest> argumentCaptor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(scenarioExecutionEngine).execute(argumentCaptor.capture());
        ExecutionRequest executionRequest = argumentCaptor.getValue();
        assertThat(executionRequest.dataset).isNotNull();
        assertThat(executionRequest.dataset.name).isEqualTo("scenarioInCampaignDataset");
        assertThat(executionRequest.tags).containsExactly("TAG");
    }

    @Test
    public void not_use_scenario_default_dataset_when_campaign_nor_scenario_in_campaign_do_not_define_dataset() {
        // Given
        TestCase gwtTestCase = GwtTestCase.builder()
            .withMetadata(
                TestCaseMetadataImpl.builder()
                    .withId("gwt")
                    .withDefaultDataset("scenarioDefaultDataset")
                    .build()
            )
            .build();

        var campaignScenarios = singletonList(new Campaign.CampaignScenario(gwtTestCase.id(), null));
        Campaign campaign = new Campaign(generateId(), "...", null, campaignScenarios, "...", false, false, null, List.of("TAG"));

        when(campaignRepository.findById(campaign.id)).thenReturn(campaign);
        when(testCaseRepository.findExecutableById(gwtTestCase.id())).thenReturn(of(gwtTestCase));
        when(executionHistoryRepository.getExecution(any(), any())).thenReturn(createExecution(gwtTestCase.id(), 42L));

        // When
        sut.executeById(campaign.id, "user");

        // Then
        ArgumentCaptor<ExecutionRequest> argumentCaptor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(scenarioExecutionEngine).execute(argumentCaptor.capture());
        ExecutionRequest executionRequest = argumentCaptor.getValue();
        assertThat(executionRequest.dataset).isEqualTo(DataSet.NO_DATASET);
        assertThat(executionRequest.tags).containsExactly("TAG");
    }

    @ParameterizedTest
    @ValueSource(strings = "__CUSTOM__") // DataSet.CUSTOM_ID
    @NullSource
    public void execute_by_id_with_inline_dataset(String datasetId) {
        // Given
        Long campaignId = 1L;
        String env = "env";
        var scenarios = Lists.list(firstTestCase.id()).stream().map(id -> new Campaign.CampaignScenario(id, null)).toList();
        Campaign campaign = new Campaign(1L, "campaign1", null, scenarios, env, false, false, "UNKNOWN", List.of("TAG"));
        var datatable = List.of(Map.of("HEADER", "VALUE"));
        var constants = Map.of("HEADER", "VALUE");
        DataSet dataSet = DataSet.builder().withId(datasetId).withName("").withDatatable(datatable).withConstants(constants).build();
        when(campaignRepository.findById(eq(1L))).thenReturn(campaign);

        // When
        CampaignExecution campaignExecution = sut.executeById(campaignId, env, dataSet, "USER");

        // Then
        assertThat(campaignExecution.dataset).satisfies(ds -> {
            assertThat(ds).isNotNull();
            assertThat(ds.id).isEqualTo(datasetId);
            assertThat(ds.constants).isEqualTo(constants);
            assertThat(ds.datatable).isEqualTo(datatable);
        });
        assertThat(campaignExecution.scenarioExecutionReports()).satisfies(report -> {
            assertThat(report).isNotEmpty();
            assertThat(report.getFirst().execution().dataset()).satisfies(ds -> {
                assertThat(ds).isPresent();
                assertThat(ds.get().constants).isEqualTo(constants);
                assertThat(ds.get().datatable).isEqualTo(datatable);
            });
        });
    }

    @Test
    public void execute_by_id_with_known_dataset() {
        // Given
        Long campaignId = 1L;
        String env = "env";
        var scenarios = Lists.list(firstTestCase.id()).stream().map(id -> new Campaign.CampaignScenario(id, null)).toList();
        Campaign campaign = new Campaign(1L, "campaign1", null, scenarios, env, false, false, "UNKNOWN", List.of("TAG"));
        DataSet dataSet = DataSet.builder().withName("").withId("DATASET_ID").build();
        when(campaignRepository.findById(eq(1L))).thenReturn(campaign);
        when(datasetRepository.findById(eq("DATASET_ID"))).thenReturn(DataSet.builder().withName("DATASET_ID").withId("DATASET_ID").build());

        // When
        CampaignExecution campaignExecution = sut.executeById(campaignId, env, dataSet, "USER");

        // Then
        assertThat(campaignExecution.dataset).isNotNull();
        assertThat(campaignExecution.dataset.constants).isEmpty();
        assertThat(campaignExecution.dataset.datatable).isEmpty();
        assertThat(campaignExecution.dataset.id).isNotNull();
        assertThat(campaignExecution.dataset.id).isEqualTo("DATASET_ID");
        assertThat(campaignExecution.scenarioExecutionReports().getFirst().execution().dataset()).isPresent();
        assertThat(campaignExecution.scenarioExecutionReports().getFirst().execution().dataset().get().id).isEqualTo("DATASET_ID");
    }

    @Test
    public void execute_by_id_with_default_dataset() {
        // Given
        Long campaignId = 1L;
        String env = "env";
        var scenarios = Lists.list(firstTestCase.id()).stream().map(id -> new Campaign.CampaignScenario(id, null)).toList();
        Campaign campaign = new Campaign(1L, "campaign1", null, scenarios, env, false, false, "DATASET_ID", List.of("TAG"));
        when(campaignRepository.findById(eq(1L))).thenReturn(campaign);
        when(datasetRepository.findById(eq("DATASET_ID"))).thenReturn(DataSet.builder().withName("DATASET_ID").withId("DATASET_ID").build());

        // When
        CampaignExecution campaignExecution = sut.executeById(campaignId, env, null, "USER");

        // Then
        assertThat(campaign.executionDataset()).isNotNull();
        assertThat(campaignExecution.dataset).isNotNull();
        assertThat(campaignExecution.dataset.constants).isEmpty();
        assertThat(campaignExecution.dataset.datatable).isEmpty();
        assertThat(campaignExecution.dataset.id).isNotNull();
        assertThat(campaignExecution.dataset.id).isEqualTo("DATASET_ID");
    }

    @Test
    public void execute_by_id_without_any_dataset() {
        // Given
        Long campaignId = 1L;
        String env = "env";
        var scenarios = Lists.list(firstTestCase.id()).stream().map(id -> new Campaign.CampaignScenario(id, null)).toList();
        Campaign campaign = new Campaign(1L, "campaign1", null, scenarios, env, false, false, null, List.of("TAG"));
        when(campaignRepository.findById(eq(1L))).thenReturn(campaign);

        // When
        CampaignExecution campaignExecution = sut.executeById(campaignId, env, null, "USER");

        // Then
        assertThat(campaign.executionDataset()).isNull();
        assertThat(campaignExecution.dataset).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "123, DEV, dataset123, user123, JIRA-1",
        "123, DEV, , user123, JIRA-1"
    })
    @DisplayName("Does not pass dataset empty if not defined in scheduling")
    void testExecuteScheduledCampaign(Long campaignId, String environment, String datasetId, String userId, String jiraId) {
        //Given
        DataSet dataset = DataSet.builder().withName("A").withId("A").build();
        var scenarios = Lists.list(firstTestCase.id()).stream().map(id -> new Campaign.CampaignScenario(id, null)).toList();
        Campaign campaign = new Campaign(1L, "campaign1", null, scenarios, "DEV", false, false, null, List.of("TAG"));
        when(campaignRepository.findById(any())).thenReturn(campaign);
        if (datasetId != null) {
            when(datasetRepository.findById(datasetId)).thenReturn(dataset);
        } else {
            when(datasetRepository.findById(any())).thenReturn(DataSet.NO_DATASET);
        }

        // When
        CampaignExecutionEngine spySut = spy(sut);
        spySut.executeScheduledCampaign(campaignId, environment, datasetId, userId, jiraId);

        // Then
        if (datasetId != null) {
            verify(spySut).executeById(campaignId, environment, dataset, userId, jiraId);
        } else {
            verify(spySut).executeById(campaignId, environment, null, userId, jiraId);
        }
    }

    private final static Random campaignIdGenerator = new Random();

    private Long generateId() {
        return (long) campaignIdGenerator.nextInt(1000);
    }

    private ExecutionHistory.Execution createExecution(String scenarioId, Long executionId) {
        return createExecution(scenarioId, executionId, SUCCESS);
    }

    private ExecutionHistory.Execution failedExecutionWithId(String scenarioId, Long executionId) {
        return createExecution(scenarioId, executionId, FAILURE);
    }

    private ExecutionHistory.Execution createExecution(String scenarioId, Long executionId, ServerReportStatus status) {
        return ImmutableExecutionHistory.Execution.builder()
            .executionId(executionId)
            .testCaseTitle("...")
            .time(LocalDateTime.now())
            .duration(3L)
            .status(status)
            .report("{\"report\":{\"status\":\"" + status + "\", \"steps\":[]}}")
            .environment("")
            .user("")
            .scenarioId(scenarioId)
            .build();
    }

    private GwtTestCase createGwtTestCase(String id) {
        return GwtTestCase.builder().withMetadata(TestCaseMetadataImpl.builder().withId(id).build()).build();
    }

    private @NotNull GwtTestCase createAndMockExecutedGwtTestCase(long id, ServerReportStatus status) {
        GwtTestCase notExecutedTestCase = createGwtTestCase(String.valueOf(id));
        when(testCaseRepository.findExecutableById(notExecutedTestCase.id())).thenReturn(of(notExecutedTestCase));
        when(executionHistoryRepository.getExecution(eq(notExecutedTestCase.id()), or(eq(0L), eq(id))))
            .thenReturn(createExecution(notExecutedTestCase.id(), id, status));
        return notExecutedTestCase;
    }

    private Campaign createCampaign() {
        return new Campaign(generateId(), "...", null, null, "campaignEnv", false, false, null, null);
    }

    private Campaign createCampaign(List<TestCase> testCases) {
        return createCampaign(testCases, false, false);
    }

    private Campaign createCampaign(List<TestCase> testCases, boolean retryAuto) {
        return createCampaign(testCases, false, retryAuto);
    }

    private Campaign createCampaign(List<TestCase> testCases, boolean parallelRun, boolean retryAuto) {
        var scenarios = testCases.stream().map(tc -> new Campaign.CampaignScenario(tc.id(), null)).toList();
        return new Campaign(1L, "campaign1", null, scenarios, "env", parallelRun, retryAuto, null, List.of("TAG"));
    }
}
