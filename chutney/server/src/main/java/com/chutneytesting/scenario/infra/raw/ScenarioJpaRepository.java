/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.scenario.infra.raw;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;

import com.chutneytesting.scenario.infra.jpa.ScenarioEntity;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ScenarioJpaRepository extends JpaRepository<ScenarioEntity, Long>, JpaSpecificationExecutor<ScenarioEntity> {

    @Query("SELECT s.version FROM SCENARIO s WHERE s.id = :id")
    Optional<Integer> lastVersion(@Param("id") Long id);

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    Optional<ScenarioEntity> findByIdAndActivated(Long id, Boolean activated);

    List<ScenarioEntity> findByActivated(Boolean activated);

    Slice<ScenarioEntity> findByActivated(Boolean activated, Pageable pageable);

    @Query("""
        SELECT new com.chutneytesting.scenario.infra.jpa.ScenarioEntity(s.id, s.title, s.description, s.tags, s.creationDate, s.activated, s.userId, s.updateDate, s.version, s.defaultDataset)
        FROM SCENARIO s
        WHERE s.id = :id
          AND s.activated = :activated
        """)
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    Optional<ScenarioEntity> findMetaDataByIdAndActivated(@Param("id") Long id, @Param("activated") Boolean activated);

    @Query("""
        SELECT new com.chutneytesting.scenario.infra.jpa.ScenarioEntity(s.id, s.title, s.description, s.tags, s.creationDate, s.activated, s.userId, s.updateDate, s.version, s.defaultDataset)
        FROM SCENARIO s
        WHERE s.activated = true
        """)
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    List<ScenarioEntity> findMetaDataByActivatedTrue();

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    List<ScenarioEntity> findByActivatedTrueAndDefaultDataset(String defaultDataset);

    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO SCENARIO (ID, TITLE, DESCRIPTION, CONTENT, TAGS, CREATION_DATE, ACTIVATED, USER_ID, UPDATE_DATE, VERSION, DEFAULT_DATASET_ID) VALUES (:id, :title, :description, :content, :tags, :creationDate, :activated, :userId, :updateDate, :version, :defaultDataset)")
    void saveWithExplicitId(
        @Param("id") Long id,
        @Param("title") String title,
        @Param("description") String description,
        @Param("content") String content,
        @Param("tags") String tags,
        @Param("creationDate") Long creationDate,
        @Param("activated") Boolean activated,
        @Param("userId") String userId,
        @Param("updateDate") Long updateDate,
        @Param("version") Integer version,
        @Param("defaultDataset") String defaultDataset);
}
