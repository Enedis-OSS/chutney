/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.scenario.infra;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.chutneytesting.scenario.domain.gwt.GwtScenario;
import com.chutneytesting.scenario.domain.gwt.GwtStep;
import com.chutneytesting.scenario.domain.gwt.GwtTestCase;
import com.chutneytesting.scenario.infra.raw.DatabaseTestCaseRepository;
import com.chutneytesting.scenario.infra.raw.ScenarioJpaRepository;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import java.util.Arrays;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableH2MemTestInfra;
import util.infra.EnablePostgreSQLTestInfra;
import util.infra.EnableSQLiteTestInfra;

public class L2CacheTest {

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

        private static final GwtTestCase GWT_TEST_CASE = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.builder().build())
            .withScenario(
                GwtScenario.builder().withWhen(GwtStep.NONE).build()
            ).build();

        @Autowired
        private DatabaseTestCaseRepository scenarioRepository;

        @Autowired
        private ScenarioJpaRepository scenarioJpaRepository;

        @Autowired
        private Environment env;

        @AfterEach
        void afterEach() {
            clearTables();
        }

        @Test
        void with_jpa_interface() {
            SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
            sessionFactory.getStatistics().setStatisticsEnabled(true);
            Statistics stats = sessionFactory.getStatistics();
            stats.clear(); // Reset stats

            String id = scenarioRepository.save(GWT_TEST_CASE);
            assertThat(stats.getSecondLevelCachePutCount()).isZero();
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isZero();
            assertThat(stats.getQueryCacheMissCount()).isZero();
            assertThat(stats.getQueryCacheHitCount()).isZero();

            scenarioJpaRepository.findByIdAndActivated(Long.parseLong(id), true);
            assertThat(stats.getSecondLevelCachePutCount()).isEqualTo(1);
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isEqualTo(1);
            assertThat(stats.getQueryCacheMissCount()).isEqualTo(1);
            assertThat(stats.getQueryCacheHitCount()).isZero();
            stats.clear();

            scenarioJpaRepository.findByIdAndActivated(Long.parseLong(id), true);
            assertThat(stats.getSecondLevelCachePutCount()).isZero();
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isZero();
            assertThat(stats.getQueryCacheMissCount()).isZero();
            assertThat(stats.getQueryCacheHitCount()).isEqualTo(1);
        }

        @Test
        @Disabled
        void create_update_scenario_with_and_without_explicit_id() {
            String explicitScenarioId = "1";
            var explicitScenario = GwtTestCase.builder().from(GWT_TEST_CASE)
                .withMetadata(
                    TestCaseMetadataImpl.builder()
                        .withId(explicitScenarioId)
                        .withDescription("Explicit id")
                        .build()
                );

            String saveId = scenarioRepository.save(explicitScenario.build());
            assertThat(saveId).isEqualTo(explicitScenarioId);
            assertThat(scenarioRepository.findById(explicitScenarioId)).isPresent();

            // scenarioRepository.removeById(saveId); // Switch this line with the underline to fail this test for PostreSQL and H2
            scenarioJpaRepository.deleteById(Long.valueOf(saveId));

            JdbcTemplate jdbcTemplate = namedParameterJdbcTemplate.getJdbcTemplate();
            jdbcTemplate.execute("DELETE FROM SCENARIO");

            assertThat(scenarioRepository.findById("1")).isNotPresent();

            var scenario = GwtTestCase.builder().from(GWT_TEST_CASE)
                .withMetadata(
                    TestCaseMetadataImpl.builder()
                        .withDescription("id from db")
                        .build()
                );
            saveId = scenarioRepository.save(scenario.build());

            if (Arrays.asList(env.getActiveProfiles()).contains("test-infra-sqlite")) {
                // SQLite does not reset intern sequence for identity column when delete (cf. line 121)
                assertThat(saveId).isEqualTo("2");
            } else {
                assertThat(saveId).isEqualTo("1");
            }

            assertThat(scenarioRepository.findById(saveId)).isPresent();

            scenario = GwtTestCase.builder().from(GWT_TEST_CASE)
                .withMetadata(
                    TestCaseMetadataImpl.builder()
                        .withId(saveId)
                        .withDescription("id from db modified")
                        .build()
                );
            scenarioRepository.save(scenario.build());
        }
    }
}
