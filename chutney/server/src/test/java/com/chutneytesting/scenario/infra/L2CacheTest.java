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
import com.chutneytesting.scenario.infra.raw.ScenarioJpaRepository;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import util.infra.AbstractLocalDatabaseTest;
import util.infra.EnableSQLiteTestInfra;

public class L2CacheTest {

    @Nested
    @EnableSQLiteTestInfra
    class SQLite extends AbstractLocalDatabaseTest {

        @Autowired
        private DatabaseTestCaseRepository scenarioRepository;

        @Autowired
        private ScenarioJpaRepository scenarioJpaRepository;

        /*
                @Test
                void with_entity_manager() {

                    // Activer les statistiques Hibernate
                    SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
                    sessionFactory.getStatistics().setStatisticsEnabled(true);
                    Statistics stats = sessionFactory.getStatistics();
                    stats.clear(); // Reset stats

                    final GwtTestCase GWT_TEST_CASE = GwtTestCase.builder()
                        .withMetadata(TestCaseMetadataImpl.builder().build())
                        .withScenario(
                            GwtScenario.builder().withWhen(GwtStep.NONE).build()
                        ).build();
                    // Insert un scénario en base
                    String id = scenarioRepository.save(GWT_TEST_CASE);

                    System.out.println("▶️ L2 Cache stats:");
                    System.out.println(" - Puts : " + stats.getSecondLevelCachePutCount());
                    System.out.println(" - Hits : " + stats.getSecondLevelCacheHitCount());
                    System.out.println(" - Miss : " + stats.getSecondLevelCacheMissCount());
                    stats.clear(); // Reset stats
                    // Première lecture - nouvelle session
                    ScenarioEntity firstRead;
                    try (EntityManager em1 = emf.createEntityManager()) {
                        em1.getTransaction().begin();
                        firstRead = em1.find(ScenarioEntity.class, id);
                        em1.getTransaction().commit();
                    }

                    System.out.println("▶️ L2 Cache stats:");
                    System.out.println(" - Puts : " + stats.getSecondLevelCachePutCount());
                    System.out.println(" - Hits : " + stats.getSecondLevelCacheHitCount());
                    System.out.println(" - Miss : " + stats.getSecondLevelCacheMissCount());
                    stats.clear(); // Reset stats
                    // Deuxième lecture - nouvelle session
                    ScenarioEntity secondRead;
                    try (EntityManager em2 = emf.createEntityManager()) {
                        em2.getTransaction().begin();
                        secondRead = em2.find(ScenarioEntity.class, id); // Doit venir du cache L2 (hit)
                        em2.getTransaction().commit();
                    }

                    System.out.println("▶️ L2 Cache stats:");
                    System.out.println(" - Puts : " + stats.getSecondLevelCachePutCount());
                    System.out.println(" - Hits : " + stats.getSecondLevelCacheHitCount());
                    System.out.println(" - Miss : " + stats.getSecondLevelCacheMissCount());
                    stats.clear(); // Reset stats
                    assertThat(firstRead).isNotNull();
                    assertThat(secondRead).isNotNull();
                    assertThat(firstRead.getId()).isEqualTo(secondRead.getId());

                    // 🔍 Vérification manuelle dans les logs :
                    // - Le premier find = put
                    // - Le second find = hit
                }

                @Test
                void with_domain_interface() {
                    // Activer les statistiques Hibernate
                    SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
                    sessionFactory.getStatistics().setStatisticsEnabled(true);
                    Statistics stats = sessionFactory.getStatistics();
                    stats.clear(); // Reset stats

                    final GwtTestCase GWT_TEST_CASE = GwtTestCase.builder()
                        .withMetadata(TestCaseMetadataImpl.builder().build())
                        .withScenario(
                            GwtScenario.builder().withWhen(GwtStep.NONE).build()
                        ).build();
                    // Insert un scénario en base
                    String id = scenarioRepository.save(GWT_TEST_CASE);

                    System.out.println("▶️ L2 Cache stats:");
                    System.out.println(" - Puts : " + stats.getSecondLevelCachePutCount());
                    System.out.println(" - Hits : " + stats.getSecondLevelCacheHitCount());
                    System.out.println(" - Miss : " + stats.getSecondLevelCacheMissCount());
                    stats.clear(); // Reset stats
                    // Première lecture - nouvelle session
                    Optional<GwtTestCase> firstRead = scenarioRepository.findById(id);

                    System.out.println("▶️ L2 Cache stats first read:");
                    System.out.println(" - Puts : " + stats.getSecondLevelCachePutCount());
                    System.out.println(" - Hits : " + stats.getSecondLevelCacheHitCount());
                    System.out.println(" - Miss : " + stats.getSecondLevelCacheMissCount());
                    stats.clear(); // Reset stats
                    // Deuxième lecture - nouvelle session
                    Optional<GwtTestCase> secondRead = scenarioRepository.findById(id);

                    System.out.println("▶️ L2 Cache stats second read:");
                    System.out.println(" - Puts : " + stats.getSecondLevelCachePutCount());
                    System.out.println(" - Hits : " + stats.getSecondLevelCacheHitCount());
                    System.out.println(" - Miss : " + stats.getSecondLevelCacheMissCount());

                    stats.clear(); // Reset stats
                    assertThat(firstRead.get().id()).isEqualTo(secondRead.get().id());

                    // 🔍 Vérification manuelle dans les logs :
                    // - Le premier find = put
                    // - Le second find = hit
                }
        */
        @Test
        void with_jpa_interface() {
            // Activer les statistiques Hibernate
            SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
            sessionFactory.getStatistics().setStatisticsEnabled(true);
            Statistics stats = sessionFactory.getStatistics();
            stats.clear(); // Reset stats

            final GwtTestCase GWT_TEST_CASE = GwtTestCase.builder()
                .withMetadata(TestCaseMetadataImpl.builder().build())
                .withScenario(
                    GwtScenario.builder().withWhen(GwtStep.NONE).build()
                ).build();

            // Insert un scénario en base
            String id = scenarioRepository.save(GWT_TEST_CASE);
            stats.clear();

            // Première lecture - nouvelle session
            Optional<ScenarioEntity> firstRead = scenarioJpaRepository.findByIdAndActivated(Long.parseLong(id), true);
            assertThat(stats.getSecondLevelCachePutCount()).isEqualTo(1);
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isEqualTo(1);
            assertThat(stats.getQueryCacheMissCount()).isEqualTo(1);
            assertThat(stats.getQueryCacheHitCount()).isZero();
            //printQueryCacheStat(stats);
            stats.clear(); // Reset stats

            // Deuxième lecture - nouvelle session
            Optional<ScenarioEntity> secondRead = scenarioJpaRepository.findByIdAndActivated(Long.parseLong(id), true);
            assertThat(stats.getSecondLevelCachePutCount()).isZero();
            assertThat(stats.getSecondLevelCacheMissCount()).isZero();
            assertThat(stats.getSecondLevelCacheHitCount()).isZero();
            assertThat(stats.getQueryCachePutCount()).isZero();
            assertThat(stats.getQueryCacheMissCount()).isZero();
            assertThat(stats.getQueryCacheHitCount()).isEqualTo(1);
            //printQueryCacheStat(stats);
            stats.clear(); // Reset stats

            assertThat(firstRead.get().getId()).isEqualTo(secondRead.get().getId());
        }

        private void printQueryCacheStat(Statistics stats) {
            //System.out.println(stats.getCacheRegionStatistics("testRegion"));
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
