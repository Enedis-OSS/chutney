/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.migration.infra;

import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExecutionReportRepository {

    private final EntityManager entityManager;

    public ExecutionReportRepository(EntityManager entityManager ) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void compressAndSaveInDb(List<ScenarioExecutionReportEntity> reportsInDb) {
        reportsInDb.forEach(report -> {
            entityManager.createQuery(
                    "UPDATE SCENARIO_EXECUTIONS_REPORTS SET report = :report WHERE id = :id")
                .setParameter("report", report.getReport())
                .setParameter("id", report.scenarioExecutionId())
                .executeUpdate();
            entityManager.detach(report);
        });
    }
}
