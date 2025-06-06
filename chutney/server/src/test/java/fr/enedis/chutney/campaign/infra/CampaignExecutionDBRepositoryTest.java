/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.of;

import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecutionReportBuilder;
import fr.enedis.chutney.server.core.domain.scenario.campaign.ScenarioExecutionCampaign;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableH2MemTestInfra;
import util.infra.EnablePostgreSQLTestInfra;
import util.infra.EnableSQLiteTestInfra;

public class CampaignExecutionDBRepositoryTest {

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
    class PostgreSQL extends AllTests {
    }

    abstract class AllTests extends AbstractLocalDatabaseTest {

        @Autowired
        private CampaignExecutionRepository sut;


        @Test
        public void should_set_start_date_and_status_on_empty_execution() {
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);

            Long campaignExecutionId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");

            CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .userId("user")
                .build();

            sut.saveCampaignExecution(campaign.id(), campaignExecution);

            CampaignExecution report = sut.getLastExecution(campaign.id());

            assertThat(report.startDate).isEqualTo(LocalDateTime.MIN);
            assertThat(report.status()).isEqualTo(ServerReportStatus.SUCCESS);
        }

        @Test
        public void should_return_the_last_campaign_execution() {
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);

            ScenarioExecutionEntity scenarioExecution = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.NOT_EXECUTED);
            ScenarioExecutionCampaign scenarioExecutionReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecution.toDomain());

            Long campaignExecutionId1 = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            Long campaignExecutionId2 = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            Long campaignExecutionId3 = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");

            CampaignExecution campaignExecution1 = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId1)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionReport)
                .userId("user")
                .build();
            CampaignExecution campaignExecution2 = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId2)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionReport)
                .userId("user")
                .build();
            CampaignExecution campaignExecution3 = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId3)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionReport)
                .userId("user")
                .build();

            sut.saveCampaignExecution(campaign.id(), campaignExecution1);
            sut.saveCampaignExecution(campaign.id(), campaignExecution2);
            sut.saveCampaignExecution(campaign.id(), campaignExecution3);

            CampaignExecution report = sut.getLastExecution(campaign.id());

            assertThat(report).isEqualTo(campaignExecution3);
        }

        @Test
        public void should_throw_exception_when_no_campaign_execution() {
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);

            assertThatThrownBy(() -> sut.getLastExecution(campaign.id()));
        }

        @Test
        public void should_persist_1_execution_when_saving_1_campaign_execution_report() {
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);

            ScenarioExecutionEntity scenarioExecution = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.NOT_EXECUTED);
            ScenarioExecutionCampaign scenarioExecutionReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecution.toDomain());
            Long campaignExecutionId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecution);

            List<CampaignExecution> reports = sut.getExecutionHistory(campaign.id());

            assertThat(reports).hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("executionId", campaignExecution.executionId)
                .hasFieldOrPropertyWithValue("campaignName", campaignExecution.campaignName)
                .hasFieldOrPropertyWithValue("partialExecution", campaignExecution.partialExecution)
                .hasFieldOrPropertyWithValue("dataset", campaignExecution.dataset)
                .hasFieldOrPropertyWithValue("executionEnvironment", campaignExecution.executionEnvironment)
                .hasFieldOrPropertyWithValue("userId", campaignExecution.userId)
            ;

            assertThat(reports.getFirst().scenarioExecutionReports()).hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("scenarioId", scenarioEntity.getId().toString())
                .hasFieldOrPropertyWithValue("scenarioName", scenarioEntity.getTitle())
                .extracting("execution")
                .hasFieldOrPropertyWithValue("executionId", scenarioExecution.id())
                .hasFieldOrPropertyWithValue("status", scenarioExecution.status())
                .hasFieldOrPropertyWithValue("environment", scenarioExecution.environment())
            ;
        }

        @Test
        public void should_persist_2_executions_when_saving_2_campaign_execution_report() {
            ScenarioEntity scenarioEntityOne = givenScenario();
            ScenarioEntity scenarioEntityTwo = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntityOne, scenarioEntityTwo);

            ScenarioExecutionEntity scenarioOneExecution = givenScenarioExecution(scenarioEntityOne.getId(), ServerReportStatus.SUCCESS);
            ScenarioExecutionCampaign scenarioOneExecutionReport = new ScenarioExecutionCampaign(scenarioEntityOne.getId().toString(), scenarioEntityOne.getTitle(), scenarioOneExecution.toDomain());
            ScenarioExecutionEntity scenarioTwoExecution = givenScenarioExecution(scenarioEntityTwo.getId(), ServerReportStatus.FAILURE);
            ScenarioExecutionCampaign scenarioTwoExecutionReport = new ScenarioExecutionCampaign(scenarioEntityTwo.getId().toString(), scenarioEntityTwo.getTitle(), scenarioTwoExecution.toDomain());
            Long campaignExecutionId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioOneExecutionReport)
                .addScenarioExecutionReport(scenarioTwoExecutionReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecution);

            List<CampaignExecution> reports = sut.getExecutionHistory(campaign.id());

            assertThat(reports).hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("executionId", campaignExecution.executionId)
                .hasFieldOrPropertyWithValue("campaignName", campaignExecution.campaignName)
                .hasFieldOrPropertyWithValue("partialExecution", campaignExecution.partialExecution)
                .hasFieldOrPropertyWithValue("dataset", campaignExecution.dataset)
                .hasFieldOrPropertyWithValue("executionEnvironment", campaignExecution.executionEnvironment)
                .hasFieldOrPropertyWithValue("userId", campaignExecution.userId)
            ;

            assertThat(reports.getFirst().scenarioExecutionReports()).hasSize(2);

            assertThat(reports.getFirst().scenarioExecutionReports()).element(0)
                .hasFieldOrPropertyWithValue("scenarioId", scenarioEntityOne.getId().toString())
                .hasFieldOrPropertyWithValue("scenarioName", scenarioEntityOne.getTitle())
                .extracting("execution")
                .hasFieldOrPropertyWithValue("executionId", scenarioOneExecution.id())
                .hasFieldOrPropertyWithValue("status", scenarioOneExecution.status())
                .hasFieldOrPropertyWithValue("environment", scenarioOneExecution.environment())
            ;
            assertThat(reports.getFirst().scenarioExecutionReports()).element(1)
                .hasFieldOrPropertyWithValue("scenarioId", scenarioEntityTwo.getId().toString())
                .hasFieldOrPropertyWithValue("scenarioName", scenarioEntityTwo.getTitle())
                .extracting("execution")
                .hasFieldOrPropertyWithValue("executionId", scenarioTwoExecution.id())
                .hasFieldOrPropertyWithValue("status", scenarioTwoExecution.status())
                .hasFieldOrPropertyWithValue("environment", scenarioTwoExecution.environment())
            ;
        }

        @Test
        public void should_remove_all_campaign_executions_when_removing_campaign_execution_report() {
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);

            ScenarioExecutionEntity scenarioExecution = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.NOT_EXECUTED);
            ScenarioExecutionCampaign scenarioExecutionReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecution.toDomain());
            Long campaignExecutionId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecution = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecution);

            sut.clearAllExecutionHistory(campaign.id());

            List<CampaignExecution> executionHistory = sut.getExecutionHistory(campaign.id());
            assertThat(executionHistory).isEmpty();

            List<?> scenarioExecutions =
                entityManager.createNativeQuery("select * from scenario_executions where id = :id", ScenarioExecutionEntity.class)
                    .setParameter("id", scenarioExecution.id())
                    .getResultList();
            assertThat(scenarioExecutions).hasSize(1);
        }

        @Test
        public void should_get_2_last_campaign_report_created() {
            clearTables();
            ScenarioEntity scenarioEntity = givenScenario();
            CampaignEntity campaign = givenCampaign(scenarioEntity);

            ScenarioExecutionEntity scenarioExecutionOne = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.NOT_EXECUTED);
            ScenarioExecutionCampaign scenarioExecutionOneReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionOne.toDomain());
            Long campaignExecutionOneId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecutionOneReport = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionOneId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionOneReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecutionOneReport);

            ScenarioExecutionEntity scenarioExecutionTwo = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.SUCCESS);
            ScenarioExecutionCampaign scenarioExecutionTwoReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionTwo.toDomain());
            Long campaignExecutionTwoId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecutionTwoReport = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionTwoId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionTwoReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecutionTwoReport);

            ScenarioExecutionEntity scenarioExecutionThree = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.FAILURE);
            ScenarioExecutionCampaign scenarioExecutionThreeReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionThree.toDomain());
            Long campaignExecutionThreeId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecutionThreeReport = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionThreeId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionThreeReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecutionThreeReport);

            ScenarioExecutionEntity scenarioExecutionFour = givenScenarioExecution(scenarioEntity.getId(), ServerReportStatus.RUNNING);
            ScenarioExecutionCampaign scenarioExecutionFourReport = new ScenarioExecutionCampaign(scenarioEntity.getId().toString(), scenarioEntity.getTitle(), scenarioExecutionFour.toDomain());
            Long campaignExecutionFourId = sut.generateCampaignExecutionId(campaign.id(), "executionEnv");
            CampaignExecution campaignExecutionFourReport = CampaignExecutionReportBuilder.builder()
                .executionId(campaignExecutionFourId)
                .campaignId(campaign.id())
                .campaignName(campaign.title())
                .partialExecution(true)
                .environment("env")
                .addScenarioExecutionReport(scenarioExecutionFourReport)
                .userId("user")
                .build();
            sut.saveCampaignExecution(campaign.id(), campaignExecutionFourReport);


            List<CampaignExecution> lastExecutions = sut.getLastExecutions(2L);

            assertThat(lastExecutions).hasSize(2);

            assertThat(lastExecutions.getFirst().scenarioExecutionReports()).hasSize(1)
                .element(0)
                .hasFieldOrPropertyWithValue("scenarioId", scenarioEntity.getId().toString())
                .hasFieldOrPropertyWithValue("scenarioName", scenarioEntity.getTitle())
                .extracting("execution")
                .hasFieldOrPropertyWithValue("executionId", scenarioExecutionFour.id())
                .hasFieldOrPropertyWithValue("status", scenarioExecutionFour.status())
            ;
            assertThat(lastExecutions.get(1).scenarioExecutionReports()).hasSize(1)
                .element(0)
                .hasFieldOrPropertyWithValue("scenarioId", scenarioEntity.getId().toString())
                .hasFieldOrPropertyWithValue("scenarioName", scenarioEntity.getTitle())
                .extracting("execution")
                .hasFieldOrPropertyWithValue("executionId", scenarioExecutionThree.id())
                .hasFieldOrPropertyWithValue("status", scenarioExecutionThree.status())
            ;
        }
    }

    @ParameterizedTest
    @MethodSource("invalidInputProvider")
    void shouldThrowExceptionForInvalidInputs(Long campaignId, String environment, Class<?> exceptionExpected) {
        CampaignExecutionRepository sut = new CampaignExecutionDBRepository(null,null,null);
        assertThatThrownBy(() -> sut.generateCampaignExecutionId(campaignId, environment))
            .isInstanceOf(exceptionExpected);
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(
            of(null, "testEnvironment", NullPointerException.class),
            of(123L, null, NullPointerException.class),
            of(123L, "", IllegalArgumentException.class)
        );
    }
}
