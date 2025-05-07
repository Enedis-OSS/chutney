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
import com.chutneytesting.scenario.infra.jpa.ScenarioEntity;
import com.chutneytesting.scenario.infra.raw.DatabaseTestCaseRepository;
import com.chutneytesting.scenario.infra.raw.DatabaseTestCaseRepositoryTest;
import com.chutneytesting.scenario.infra.raw.ScenarioJpaRepository;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

        @Autowired
        private DatabaseTestCaseRepository scenarioRepository;

        @Autowired
        private ScenarioJpaRepository scenarioJpaRepository;

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

            final GwtTestCase GWT_TEST_CASE = GwtTestCase.builder()
                .withMetadata(TestCaseMetadataImpl.builder().build())
                .withScenario(
                    GwtScenario.builder().withWhen(GwtStep.NONE).build()
                ).build();

            String id = scenarioRepository.save(GWT_TEST_CASE);

            Optional<ScenarioEntity> firstRead = scenarioJpaRepository.findByIdAndActivated(Long.parseLong(id), true);
            assertThat(stats.getSecondLevelCachePutCount()).isEqualTo(1);
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isEqualTo(1);
            assertThat(stats.getQueryCacheMissCount()).isEqualTo(1);
            assertThat(stats.getQueryCacheHitCount()).isZero();
            stats.clear();

            Optional<ScenarioEntity> secondRead = scenarioJpaRepository.findByIdAndActivated(Long.parseLong(id), true);
            assertThat(stats.getSecondLevelCachePutCount()).isZero();
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isZero();
            assertThat(stats.getQueryCacheMissCount()).isZero();
            assertThat(stats.getQueryCacheHitCount()).isEqualTo(1);
        }

        private void printQueryCacheStat(Statistics stats) {
            System.out.println(" - L2 cache Puts : " + stats.getSecondLevelCachePutCount());
            System.out.println(" - L2 cache Hits : " + stats.getSecondLevelCacheHitCount());
            System.out.println(" - L2 cache Miss : " + stats.getSecondLevelCacheMissCount());
            System.out.println(" - Query cache Puts : " + stats.getQueryCachePutCount());
            System.out.println(" - Query cache Hits : " + stats.getQueryCacheHitCount());
            System.out.println(" - Query cache Miss : " + stats.getQueryCacheMissCount());
            System.out.println(" - Query ts cache Puts : " + stats.getUpdateTimestampsCachePutCount());
            System.out.println(" - Query ts cache Hits : " + stats.getUpdateTimestampsCacheHitCount());
            System.out.println(" - Query ts cache Miss : " + stats.getUpdateTimestampsCacheMissCount());
        }
    }
}
