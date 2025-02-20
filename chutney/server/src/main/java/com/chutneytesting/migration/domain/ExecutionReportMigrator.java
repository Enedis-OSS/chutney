/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.migration.domain;

import com.chutneytesting.execution.infra.storage.ScenarioExecutionReportJpaRepository;
import com.chutneytesting.execution.infra.storage.index.ExecutionReportIndexRepository;
import com.chutneytesting.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import com.chutneytesting.migration.infra.ExecutionReportRepository;
import com.chutneytesting.scenario.infra.jpa.ScenarioEntity;
import com.chutneytesting.scenario.infra.raw.ScenarioJpaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class ExecutionReportMigrator implements DataMigrator {

    private final ScenarioExecutionReportJpaRepository scenarioExecutionReportJpaRepository;
    private final ExecutionReportIndexRepository executionReportIndexRepository;
    private final ExecutionReportRepository executionReportRepository;
    private final ScenarioJpaRepository scenarioJpaRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionReportMigrator.class);
    private List<String> activatedScenariosIds;

    public ExecutionReportMigrator(ExecutionReportRepository executionReportRepository,
                                   ScenarioExecutionReportJpaRepository scenarioExecutionReportJpaRepository,
                                   ExecutionReportIndexRepository executionReportIndexRepository,
                                   ScenarioJpaRepository scenarioJpaRepository) {
        this.scenarioExecutionReportJpaRepository = scenarioExecutionReportJpaRepository;
        this.scenarioJpaRepository = scenarioJpaRepository;
        this.executionReportIndexRepository = executionReportIndexRepository;
        this.executionReportRepository = executionReportRepository;
    }

    @Override
    public void migrate() {
        if (isMigrationDone()) {
            LOGGER.info("Report index not empty. Skipping indexing and in-db compression...");
            return;
        }
        LOGGER.info("Start indexing and in-db compression...");
        List<ScenarioEntity> activeScenarios = scenarioJpaRepository.findByActivated(true);
        activatedScenariosIds = activeScenarios.stream().map(scenarioEntity -> scenarioEntity.getId().toString()).toList();
        PageRequest firstPage = PageRequest.of(0, 10);
        int count = 0;
        migrate(firstPage, count);
        activatedScenariosIds = null;
    }

    private void migrate(Pageable pageable, int previousCount) {
        LOGGER.debug("Indexing and compressing reports in page n° {}", pageable.getPageNumber());
        Slice<ScenarioExecutionReportEntity> slice = scenarioExecutionReportJpaRepository.findByScenarioExecutionScenarioIdIn(activatedScenariosIds, pageable);
        List<ScenarioExecutionReportEntity> reports = slice.getContent();

        executionReportRepository.compressAndSaveInDb(reports);
        index(reports);

        int count = previousCount + slice.getNumberOfElements();
        if (slice.hasNext()) {
            migrate(slice.nextPageable(), count);
        } else {
            LOGGER.info("{} report(s) successfully compressed and indexed", count);
        }
    }

    private void index(List<ScenarioExecutionReportEntity> reportsInDb) {
        executionReportIndexRepository.saveAll(reportsInDb);
    }

    private boolean isMigrationDone() {
        int indexedReports = executionReportIndexRepository.count();
        return indexedReports > 0;
    }
}
