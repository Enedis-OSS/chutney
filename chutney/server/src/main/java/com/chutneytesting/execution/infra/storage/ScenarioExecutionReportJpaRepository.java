/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.execution.infra.storage;

import com.chutneytesting.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScenarioExecutionReportJpaRepository extends JpaRepository<ScenarioExecutionReportEntity, Long>, JpaSpecificationExecutor<ScenarioExecutionReportEntity> {
    ScenarioExecutionReportEntity findByScenarioExecutionId(Long scenarioExecutionId);
    Slice<ScenarioExecutionReportEntity> findByScenarioExecutionScenarioIdIn(List<String> scenarioExecutionScenarioId, Pageable pageable);
}
