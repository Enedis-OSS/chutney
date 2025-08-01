/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.storage;

import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.FAILURE;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.NOT_EXECUTED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.PAUSED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.RUNNING;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.STOPPED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.SUCCESS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import fr.enedis.chutney.campaign.infra.CampaignExecutionDBRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionNotFoundException;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.scenario.infra.raw.DatabaseTestCaseRepository;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.DetachedExecution;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.Execution;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ReportNotFoundException;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecutionReportBuilder;
import fr.enedis.chutney.server.core.domain.scenario.campaign.ScenarioExecutionCampaign;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import org.hibernate.exception.LockAcquisitionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.NestedTestConfiguration;
import org.sqlite.SQLiteException;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableH2MemTestInfra;
import util.infra.EnablePostgreSQLTestInfra;
import util.infra.EnableSQLiteTestInfra;

public class DatabaseExecutionHistoryRepositoryTest {

    @Nested
    @EnableH2MemTestInfra
    class H2 extends AllTests {
    }

    @Nested
    @EnableSQLiteTestInfra
    class SQLite extends AllTests {
    }

    @Nested
    @EnablePostgreSQLTestInfra
    class PostreSQL extends AllTests {
    }

    abstract class AllTests extends AbstractLocalDatabaseTest {
        @Autowired
        private DatabaseExecutionHistoryRepository sut;

        @Autowired
        private CampaignExecutionDBRepository campaignExecutionDBRepository;

        @Autowired
        @Qualifier("reportObjectMapper")
        private ObjectMapper objectMapper;

        @Autowired
        private ScenarioExecutionReportJpaRepository scenarioExecutionReportJpaRepository;

        @Autowired
        private DatabaseTestCaseRepository databaseTestCaseRepository;

        @AfterEach
        void afterEach() {
            clearTables();
        }

        @Test
        public void parallel_execution_does_not_lock_database() throws InterruptedException {
            int numThreads = 10;
            // Given n parallels scenarios
            List<String> ids = new ArrayList<>(numThreads);
            for (int i = 0; i < numThreads; i++) {
                String id = givenScenario().getId().toString();
                ids.add(id);
                sut.store(id, buildDetachedExecution(RUNNING, "exec", ""));
            }

            // Use a latch to sync all threads
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);

            List<Throwable> throwns = new ArrayList<>(numThreads);
            ids.forEach(((id) -> {
                ExecutionSummary summary = sut.getExecutions(id).getFirst();
                Thread t = new Thread(() -> {
                    try {
                        startLatch.await();
                        throwns.add(catchThrowable(() ->
                            sut.update(id, buildDetachedExecution(SUCCESS, "updated", "").attach(summary.executionId(), id))
                        ));
                    } catch (InterruptedException e) {
                        // do nothing
                    } finally {
                        endLatch.countDown();
                    }
                });
                t.start();
            }));

            startLatch.countDown(); // Starts all threads
            endLatch.await(); // await termination

            assertThat(throwns).doesNotHaveAnyElementsOfTypes(
                CannotAcquireLockException.class,
                LockAcquisitionException.class,
                SQLiteException.class
            );
        }

        @Test
        public void execution_summary_is_available_after_storing_sorted_newest_first() {
            String scenarioId = givenScenarioId();
            sut.store(scenarioId, buildDetachedExecution(SUCCESS, "exec1", ""));
            sut.store(scenarioId, buildDetachedExecution(SUCCESS, "exec2", ""));
            sut.store(scenarioId, buildDetachedExecution(FAILURE, "exec3", ""));

            assertThat(sut.getExecutions(scenarioId))
                .extracting(summary -> summary.info().get()).containsExactly("exec3", "exec2", "exec1");
        }

        @Test
        public void last_execution_return_newest_first() {
            String scenarioIdOne = givenScenarioId();
            sut.store(scenarioIdOne, buildDetachedExecution(SUCCESS, "exec1", ""));
            sut.store(scenarioIdOne, buildDetachedExecution(SUCCESS, "exec2", ""));
            sut.store(scenarioIdOne, buildDetachedExecution(FAILURE, "exec3", ""));

            String scenarioIdTwo = givenScenarioId();
            sut.store(scenarioIdTwo, buildDetachedExecution(SUCCESS, "exec6", ""));
            sut.store(scenarioIdTwo, buildDetachedExecution(SUCCESS, "exec5", ""));
            sut.store(scenarioIdTwo, buildDetachedExecution(FAILURE, "exec4", ""));

            Map<String, ExecutionSummary> lastExecutions = sut.getLastExecutions(List.of(scenarioIdOne, scenarioIdTwo));
            assertThat(lastExecutions).containsOnlyKeys(scenarioIdOne, scenarioIdTwo);
            assertThat(lastExecutions.get(scenarioIdOne).info()).hasValue("exec3");
            assertThat(lastExecutions.get(scenarioIdTwo).info()).hasValue("exec4");
        }

        @Test
        public void last_execution_does_not_return_not_executed() {
            String scenarioIdOne = givenScenarioId();
            sut.store(scenarioIdOne, buildDetachedExecution(SUCCESS, "exec1", ""));
            sut.store(scenarioIdOne, buildDetachedExecution(NOT_EXECUTED, "exec2", ""));

            Map<String, ExecutionSummary> lastExecutions = sut.getLastExecutions(List.of(scenarioIdOne, scenarioIdOne));
            assertThat(lastExecutions).containsOnlyKeys(scenarioIdOne);
            assertThat(lastExecutions.get(scenarioIdOne).info()).hasValue("exec1");
            assertThat(lastExecutions.get(scenarioIdOne).status()).isEqualTo(SUCCESS);
        }


        @Test
        public void last_execution_return_last_running_even_if_it_is_not_the_last_exec() {
            String scenarioIdOne = givenScenarioId();
            sut.store(scenarioIdOne, buildDetachedExecution(RUNNING, "exec1", ""));
            sut.store(scenarioIdOne, buildDetachedExecution(RUNNING, "exec2", ""));
            sut.store(scenarioIdOne, buildDetachedExecution(SUCCESS, "exec3", ""));

            Map<String, ExecutionSummary> lastExecutions = sut.getLastExecutions(List.of(scenarioIdOne));
            assertThat(lastExecutions).containsOnlyKeys(scenarioIdOne);
            assertThat(lastExecutions.get(scenarioIdOne).info()).hasValue("exec2");
            assertThat(lastExecutions.get(scenarioIdOne).status()).isEqualTo(RUNNING);
        }

        @Test
        public void storage_keeps_all_items() {
            String scenarioId = givenScenarioId();
            DetachedExecution execution = buildDetachedExecution(SUCCESS, "", "");
            IntStream.range(0, 23).forEach(i -> sut.store(scenarioId, execution));

            assertThat(sut.getExecutions("-1")).hasSize(0);

            Number executionsCount = (Number) entityManager.createNativeQuery(
                "SELECT count(*) as count FROM SCENARIO_EXECUTIONS WHERE SCENARIO_ID = '" + scenarioId + "'").getSingleResult();

            assertThat(executionsCount.intValue())
                .as("All 23 reports of test scenario")
                .isEqualTo(23);
        }

        @Test
        public void update_execution_alters_last_one() {
            String scenarioId = givenScenario().getId().toString();
            sut.store(scenarioId, buildDetachedExecution(RUNNING, "exec", ""));

            ExecutionSummary last = sut.getExecutions(scenarioId).getFirst();
            assertThat(last.status()).isEqualTo(RUNNING);
            assertThat(last.info()).hasValue("exec");

            sut.update(scenarioId, buildDetachedExecution(SUCCESS, "updated", "").attach(last.executionId(), scenarioId));

            Execution updatedExecution = sut.getExecution(scenarioId, last.executionId());
            assertThat(updatedExecution.status()).isEqualTo(SUCCESS);
            assertThat(updatedExecution.info()).hasValue("updated");
        }

        @Disabled("TODO - Failed sometimes - investigation has to be done")
        @Test
        public void update_preserve_other_executions_order() {
            String scenarioId = givenScenarioId();
            sut.store(scenarioId, buildDetachedExecution(FAILURE, "exec1", ""));
            sut.store(scenarioId, buildDetachedExecution(SUCCESS, "exec2", ""));
            sut.store(scenarioId, buildDetachedExecution(RUNNING, "exec3", ""));

            ExecutionSummary last = sut.getExecutions(scenarioId).getFirst();
            assertThat(last.status()).isEqualTo(RUNNING);
            assertThat(last.info()).contains("exec3");

            sut.update(scenarioId, buildDetachedExecution(SUCCESS, "updated", "").attach(last.executionId(), scenarioId));

            assertThat(
                sut.getExecutions(scenarioId).stream()
                    .map(ExecutionHistory.ExecutionProperties::info)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
            ).containsExactly("updated", "exec2", "exec1");
        }

        @Test
        public void update_on_empty_history_throws() {
            String scenarioId = givenScenarioId();
            long unknownExecutionId = -1L;
            assertThatExceptionOfType(ReportNotFoundException.class)
                .isThrownBy(() -> sut.update(scenarioId, buildDetachedExecution(SUCCESS, "updated", "").attach(unknownExecutionId, scenarioId)))
                .withMessage("Unable to find report " + unknownExecutionId + " of scenario " + scenarioId);
        }

        @Test
        public void all_running_executions_are_set_to_KO_on_startup() {
            // Given running executions
            String scenarioIdOne = givenScenarioId();
            String scenarioIdTwo = givenScenarioId();
            sut.store(scenarioIdOne, buildDetachedExecution(RUNNING, "exec1", ""));
            sut.store(scenarioIdTwo, buildDetachedExecution(RUNNING, "exec2", ""));

            // When
            int nbOfAffectedExecutions = sut.setAllRunningExecutionsToKO();

            // Then, these executions are KO
            assertThat(nbOfAffectedExecutions).isEqualTo(2);
            assertThat(sut.getExecutions(scenarioIdOne).getFirst().status()).isEqualTo(FAILURE);
            assertThat(sut.getExecutions(scenarioIdTwo).getFirst().status()).isEqualTo(FAILURE);

            // And there is no more running execution
            assertThat(sut.getExecutionsWithStatus(RUNNING).size()).isEqualTo(0);
        }

        @Test
        public void all_running_executions_are_set_to_KO_on_startup_check_report_status_update() throws JsonProcessingException {
            // Given running executions
            String scenarioIdOne = "123";
            Long scenarioId = sut.store(scenarioIdOne, buildDetachedExecution(RUNNING, "exec1", "")).summary().executionId();

            // When
            int nbOfAffectedExecutions = sut.setAllRunningExecutionsToKO();

            // Then, these executions are KO
            assertThat(nbOfAffectedExecutions).isEqualTo(1);
            assertThat(sut.getExecutions(scenarioIdOne).getFirst().status()).isEqualTo(FAILURE);
            ScenarioExecutionReportEntity scenarioExecutionReport = scenarioExecutionReportJpaRepository.findById(scenarioId).orElseThrow();
            ScenarioExecutionReport report = objectMapper.readValue(scenarioExecutionReport.getReport(), ScenarioExecutionReport.class);
            assertThat(report.report.status).isEqualTo(SUCCESS);
            assertThat(report.report.steps.size()).isEqualTo(1);
            assertThat(report.report.steps.getFirst().status).isEqualTo(STOPPED);
            assertThat(report.report.steps.getFirst().steps.size()).isEqualTo(1);
            assertThat(report.report.steps.getFirst().steps.getFirst().status).isEqualTo(STOPPED);

            // And there is no more running execution
            assertThat(sut.getExecutionsWithStatus(RUNNING).size()).isEqualTo(0);
        }

        @Test
        public void all_paused_executions_are_set_to_KO_on_startup_check_report_status_update() throws JsonProcessingException {
            // Given running executions
            String scenarioIdOne = "123";
            Long scenarioId = sut.store(scenarioIdOne, buildDetachedExecution(PAUSED, "exec1", "")).summary().executionId();

            // When
            int nbOfAffectedExecutions = sut.setAllRunningExecutionsToKO();

            // Then, these executions are KO
            assertThat(nbOfAffectedExecutions).isEqualTo(1);
            ExecutionSummary execution = sut.getExecutions(scenarioIdOne).getFirst();
            assertThat(execution.status()).isEqualTo(FAILURE);
            assertThat(execution.tags()).hasValue(defaultScenarioTags());
            ScenarioExecutionReportEntity scenarioExecutionReport = scenarioExecutionReportJpaRepository.findById(scenarioId).orElseThrow();
            ScenarioExecutionReport report = objectMapper.readValue(scenarioExecutionReport.getReport(), ScenarioExecutionReport.class);
            assertThat(report.report.status).isEqualTo(SUCCESS);
            assertThat(report.report.steps.size()).isEqualTo(1);
            assertThat(report.report.steps.getFirst().status).isEqualTo(STOPPED);
            assertThat(report.report.steps.getFirst().steps.size()).isEqualTo(1);
            assertThat(report.report.steps.getFirst().steps.getFirst().status).isEqualTo(STOPPED);

            // And there is no more running execution
            assertThat(sut.getExecutionsWithStatus(PAUSED).size()).isEqualTo(0);
        }

        @Test
        public void getExecution_throws_when_not_found() {
            assertThatExceptionOfType(ReportNotFoundException.class)
                .isThrownBy(() -> sut.getExecution("-1", 42L))
                .withMessage("Unable to find report 42 of scenario -1");
        }

        @Test
        public void getExecution_throws_when_exist_but_not_on_this_scenario() {
            String scenarioId = givenScenario().getId().toString();
            Execution executionCreated = sut.store(scenarioId, buildDetachedExecution(RUNNING, "exec1", ""));

            assertThat(sut.getExecution(scenarioId, executionCreated.executionId())).isNotNull();

            assertThatExceptionOfType(ReportNotFoundException.class)
                .isThrownBy(() -> sut.getExecution("-1", executionCreated.executionId()))
                .withMessage("Unable to find report " + executionCreated.executionId() + " of scenario -1");
        }

        @Test
        public void truncate_report_info_and_error_on_save_or_update() {
            String scenarioId = givenScenarioId();
            final String tooLongString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede.";

            Execution last = sut.store(scenarioId, buildDetachedExecution(SUCCESS, tooLongString, tooLongString));

            assertThat(sut.getExecutions(scenarioId).getFirst().info())
                .hasValueSatisfying(v -> assertThat(v).hasSize(512));

            assertThat(sut.getExecutions(scenarioId).getFirst().error())
                .hasValueSatisfying(v -> assertThat(v).hasSize(512));

            sut.update(scenarioId, buildDetachedExecution(SUCCESS, tooLongString, tooLongString).attach(last.executionId(), scenarioId));

            assertThat(sut.getExecutions(scenarioId).getFirst().info())
                .hasValueSatisfying(v -> assertThat(v).hasSize(512));

            assertThat(sut.getExecutions(scenarioId).getFirst().error())
                .hasValueSatisfying(v -> assertThat(v).hasSize(512));
        }

        @Test
        public void map_custom_dataset_to_custom_id() {
            String scenarioId = givenScenarioId();

            ImmutableExecutionHistory.DetachedExecution detachedExecution = ImmutableExecutionHistory.DetachedExecution.builder()
                .time(LocalDateTime.now())
                .duration(12L)
                .status(SUCCESS)
                .report("report")
                .testCaseTitle("Fake title")
                .environment("")
                .dataset(DataSet.builder().withId(null).withName("").build())
                .user("")
                .build();

            sut.store(scenarioId, detachedExecution);

            DataSet dataSet = sut.getExecutions(scenarioId).getFirst().dataset().get();
            assertThat(dataSet.id).isEqualTo("__CUSTOM__");
        }

        @Test
        public void map_campaign_only_when_executing_from_campaign() {
            // Given
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);
            ScenarioExecutionEntity scenarioExecutionOne = givenScenarioExecution(scenarioEntity.getId(), FAILURE);
            ScenarioExecutionCampaign scenarioExecutionOneReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionOne.toDomain());
            ScenarioExecutionEntity scenarioExecutionTwo = givenScenarioExecution(scenarioEntity.getId(), SUCCESS);

            Long campaignExecutionId = campaignExecutionDBRepository.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionOneReport)
                .userId("user")
                .dataset(DataSet.builder().withId("ds287").withName("").build())
                .build();
            campaignExecutionDBRepository.saveCampaignExecution(campaign.id(), campaignExecution);

            // When
            List<ExecutionSummary> executions = sut.getExecutions(scenarioEntity.getId().toString());

            // Then
            assertThat(executions).hasSize(2);
            assertThat(executions.getFirst().executionId()).isEqualTo(scenarioExecutionTwo.id());
            assertThat(executions.getFirst().campaignReport()).isEmpty();
            assertThat(executions.get(1).executionId()).isEqualTo(scenarioExecutionOne.id());
            assertThat(executions.get(1).campaignReport()).hasValueSatisfying(report -> {
                assertThat(report.campaignId).isEqualTo(campaign.id());
                assertThat(report.executionId).isEqualTo(campaignExecutionId);
            });
        }

        @Test
        public void retrieve_scenario_execution_summary() {
            // Given
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);
            ScenarioExecutionEntity scenarioExecutionOne = givenScenarioExecution(scenarioEntity.getId(), FAILURE);
            ScenarioExecutionCampaign scenarioExecutionOneReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionOne.toDomain());
            givenScenarioExecution(scenarioEntity.getId(), SUCCESS);

            Long campaignExecutionId = campaignExecutionDBRepository.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionOneReport)
                .userId("user")
                .dataset(DataSet.builder().withId("ds287").withName("").build())
                .build();
            campaignExecutionDBRepository.saveCampaignExecution(campaign.id(), campaignExecution);

            // When
            ExecutionSummary executionSummary = sut.getExecutionSummary(scenarioExecutionOne.id());

            // Then
            assertThat(executionSummary.executionId()).isEqualTo(scenarioExecutionOne.id());
            assertThat(executionSummary.tags()).hasValue(defaultScenarioTags());
            assertThat(executionSummary.campaignReport()).isPresent();
            assertThat(executionSummary.campaignReport()).hasValueSatisfying(cr -> {
                assertThat(cr.campaignId).isEqualTo(campaign.id());
                assertThat(cr.executionId).isEqualTo(campaignExecutionId);
            });
        }

        @Test
        void deletes_executions_by_ids() {
            String scenarioId = givenScenarioId();
            Execution exec1 = sut.store(scenarioId, buildDetachedExecution(SUCCESS, "exec1", ""));
            Execution exec2 = sut.store(scenarioId, buildDetachedExecution(FAILURE, "exec2", ""));
            Execution exec3 = sut.store(scenarioId, buildDetachedExecution(RUNNING, "exec3", ""));
            Execution exec4 = sut.store(scenarioId, buildDetachedExecution(PAUSED, "exec4", ""));

            var report = sut.deleteExecutions(Set.of(exec1.executionId(), exec2.executionId(), exec3.executionId(), exec4.executionId()));

            assertThat(report.campaignsExecutionsIds()).isEmpty();
            assertThat(report.scenariosExecutionsIds()).hasSize(2);
            List.of(exec1.executionId(), exec2.executionId()).forEach(executionId -> assertThatThrownBy(() ->
                sut.getExecutionSummary(executionId)
            ).isInstanceOf(ReportNotFoundException.class));

            List.of(exec3.executionId(), exec4.executionId()).forEach(executionId ->
                assertThat(sut.getExecutionSummary(executionId).executionId()).isEqualTo(executionId)
            );
        }

        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
        @DisplayName("Delete associated finished campaign execution when scenario execution is the only one left")
        class scenarioExecutionDelete {

            @ParameterizedTest
            @EnumSource(ServerReportStatus.class)
            void campaign_execution_with_only_one_scenario_execution(ServerReportStatus executionStatus) {
                // GIVEN
                ScenarioEntity scenarioEntity = givenScenario();
                CampaignEntity campaign = givenCampaign(scenarioEntity);
                ScenarioExecutionEntity scenarioExecutionOne = givenScenarioExecution(scenarioEntity.getId(), executionStatus);
                ScenarioExecutionCampaign scenarioExecutionOneReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionOne.toDomain());

                Long campaignExecutionId = campaignExecutionDBRepository.generateCampaignExecutionId(campaign.id(), "env");
                CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                    .executionId(campaignExecutionId)
                    .campaignId(campaign.id())
                    .campaignName(campaign.title())
                    .environment("env")
                    .addScenarioExecutionReport(scenarioExecutionOneReport)
                    .userId("user")
                    .status(executionStatus)
                    .build();
                campaignExecutionDBRepository.saveCampaignExecution(campaign.id(), campaignExecution);

                // WHEN
                var report = sut.deleteExecutions(Set.of(scenarioExecutionOne.toDomain().executionId()));

                // THEN
                if (executionStatus.isFinal()) {
                    assertThat(report.campaignsExecutionsIds()).hasSize(1);
                    assertThat(report.scenariosExecutionsIds()).hasSize(1);
                    assertThatThrownBy(() -> campaignExecutionDBRepository.getCampaignExecutionById(campaignExecutionId))
                        .isInstanceOf(CampaignExecutionNotFoundException.class);
                } else {
                    assertThat(report.campaignsExecutionsIds()).isEmpty();
                    assertThat(report.scenariosExecutionsIds()).isEmpty();
                    assertThat(campaignExecutionDBRepository.getCampaignExecutionById(campaignExecutionId)).isNotNull();
                }
            }

            @Test
            void campaign_execution_with_many_scenario_execution() {
                // GIVEN
                ScenarioEntity scenarioEntity = givenScenario();
                CampaignEntity campaign = givenCampaign(scenarioEntity);
                ScenarioExecutionEntity scenarioExecutionOne = givenScenarioExecution(scenarioEntity.getId(), FAILURE);
                ScenarioExecutionCampaign scenarioExecutionOneReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionOne.toDomain());
                ScenarioExecutionEntity scenarioExecutionTwo = givenScenarioExecution(scenarioEntity.getId(), SUCCESS);
                ScenarioExecutionCampaign scenarioExecutionTwoReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionTwo.toDomain());

                Long campaignExecutionId = campaignExecutionDBRepository.generateCampaignExecutionId(campaign.id(), "env");
                CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                    .executionId(campaignExecutionId)
                    .campaignId(campaign.id())
                    .campaignName(campaign.title())
                    .partialExecution(true)
                    .environment("env")
                    .addScenarioExecutionReport(scenarioExecutionOneReport)
                    .addScenarioExecutionReport(scenarioExecutionTwoReport)
                    .userId("user")
                    .dataset(DataSet.builder().withName("ds287").build())
                    .build();
                campaignExecutionDBRepository.saveCampaignExecution(campaign.id(), campaignExecution);

                // WHEN
                var report = sut.deleteExecutions(Set.of(scenarioExecutionOne.id()));

                // THEN
                assertThat(report.campaignsExecutionsIds()).isEmpty();
                assertThat(report.scenariosExecutionsIds()).hasSize(1);
                List.of(scenarioExecutionOne.id()).forEach(executionId -> assertThatThrownBy(() -> sut.getExecutionSummary(executionId))
                    .isInstanceOf(ReportNotFoundException.class));

                assertThat(campaignExecutionDBRepository.getCampaignExecutionById(campaignExecutionId).scenarioExecutionReports()).hasSize(1);
                assertThat(campaignExecutionDBRepository.getCampaignExecutionById(campaignExecutionId).scenarioExecutionReports().getFirst().execution().executionId()).isEqualTo(scenarioExecutionTwo.id());

            }

        }

        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
        @DisplayName("Find scenario execution with report match")
        class ScenarioExecutionReportMatch {
            @AfterEach
            void afterEach() {
                clearTables();
            }

            @Test
            void simple_case() {
                var scenarioId1 = givenScenario().getId().toString();
                var scenarioId2 = givenScenario().getId().toString();
                var exec1 = sut.store(scenarioId1, buildDetachedExecution("toto"));
                sut.store(scenarioId2, buildDetachedExecution("tutu"));

                var executionSummaryList = sut.getExecutionReportMatchKeyword("to");

                assertThat(executionSummaryList).hasSize(1);
                assertThat(executionSummaryList.getFirst().executionId()).isEqualTo(exec1.executionId());
                assertThat(executionSummaryList.getFirst().scenarioId()).isEqualTo(exec1.scenarioId());
            }

            @Test
            void filter_unactivated_scenario_execution() {
                var scenarioId1 = givenScenario().getId().toString();
                var scenarioId2 = givenScenario().getId().toString();
                var exec1 = sut.store(scenarioId1, buildDetachedExecution("toto"));
                sut.store(scenarioId2, buildDetachedExecution("tutu"));
                databaseTestCaseRepository.removeById(scenarioId2);

                var executionSummaryList = sut.getExecutionReportMatchKeyword("t");

                assertThat(executionSummaryList).hasSize(1);
                assertThat(executionSummaryList.getFirst().executionId()).isEqualTo(exec1.executionId());
                assertThat(executionSummaryList.getFirst().scenarioId()).isEqualTo(exec1.scenarioId());
            }

            @Test
            void limit_results_to_100() {
                IntStream.range(0, 110).forEach(i -> {
                    String scenarioId = givenScenario().getId().toString();
                    sut.store(scenarioId, buildDetachedExecution("report"));
                });

                var executionSummaryList = sut.getExecutionReportMatchKeyword("ort");

                assertThat(executionSummaryList).hasSize(100);
            }

            @Test
            void order_by_id_descending() {
                List<Long> executionsIds = new ArrayList<>();
                IntStream.range(0, 10).forEach(i -> {
                    var scenarioId = givenScenario().getId();
                    var execution = sut.store(scenarioId.toString(), buildDetachedExecution("report"));
                    executionsIds.add(execution.executionId());
                });
                var expectedOrder = executionsIds.stream().sorted(Comparator.<Long>naturalOrder().reversed()).toList();

                var executionSummaryList = sut.getExecutionReportMatchKeyword("ort");

                assertThat(executionSummaryList)
                    .map(ExecutionSummary::executionId)
                    .containsExactlyElementsOf(expectedOrder);
            }

            private DetachedExecution buildDetachedExecution(String report) {
                return ImmutableExecutionHistory.DetachedExecution.builder()
                    .time(LocalDateTime.now())
                    .duration(12L)
                    .status(SUCCESS)
                    .report(report)
                    .testCaseTitle("Fake title")
                    .environment("")
                    .dataset(DataSet.builder().withId("fake dataset id").withName("").build())
                    .user("")
                    .build();
            }
        }

        private DetachedExecution buildDetachedExecution(ServerReportStatus status, String info, String error) {
            return ImmutableExecutionHistory.DetachedExecution.builder()
                .time(LocalDateTime.now())
                .duration(12L)
                .status(status)
                .info(info)
                .error(error)
                .report(buildReport())
                .testCaseTitle("Fake title")
                .environment("")
                .dataset(DataSet.builder().withId("fake dataset id").withName("").build())
                .user("")
                .tags(defaultScenarioTags())
                .build();
        }

        private String buildReport() {
            StepExecutionReportCore successStepReport =
                stepReport("root step Title", -1L, SUCCESS,
                    stepReport("step 1", 24L, PAUSED,
                        stepReport("step1.1", 23L, RUNNING)));
            DataSet dataSet = DataSet.builder().withId("id").withName("ds").withConstants(Map.of("key", "value")).withDatatable(List.of(Map.of("A", "A1", "B", "B1"))).build();
            try {
                return objectMapper.writeValueAsString(new ScenarioExecutionReport(1L, "scenario name", "", "", null, dataSet, successStepReport));
            } catch (JsonProcessingException exception) {
                return "";
            }
        }

        private StepExecutionReportCore stepReport(String title, long duration, ServerReportStatus status, StepExecutionReportCore... subSteps) {
            List<String> infos = SUCCESS == status ? singletonList("test info") : emptyList();
            List<String> errors = FAILURE == status ? singletonList("test error") : emptyList();

            return new StepExecutionReportCore(
                title,
                duration,
                Instant.now(),
                status,
                infos,
                errors,
                Arrays.asList(subSteps),
                "type",
                "targetName",
                "targetUrl",
                "strategy",
                Maps.newHashMap(),
                Maps.newHashMap());
        }
    }
}
