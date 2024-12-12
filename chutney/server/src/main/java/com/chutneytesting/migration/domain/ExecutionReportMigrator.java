/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.migration.domain;

import com.chutneytesting.execution.infra.storage.ScenarioExecutionReportJpaRepository;
import com.chutneytesting.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import com.chutneytesting.index.infra.ScenarioExecutionReportIndexRepository;
import com.chutneytesting.migration.infra.ExecutionReportRepository;
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
    private final ScenarioExecutionReportIndexRepository scenarioExecutionReportIndexRepository;
    private final ExecutionReportRepository executionReportRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionReportMigrator.class);

    public ExecutionReportMigrator(ExecutionReportRepository executionReportRepository,
                                   ScenarioExecutionReportJpaRepository scenarioExecutionReportJpaRepository, ScenarioExecutionReportIndexRepository scenarioExecutionReportIndexRepository) {
        this.scenarioExecutionReportJpaRepository = scenarioExecutionReportJpaRepository;
        this.executionReportRepository = executionReportRepository;
        this.scenarioExecutionReportIndexRepository = scenarioExecutionReportIndexRepository;
    }

    @Override
    public void migrate() {
        if (isMigrationDone()) {
            LOGGER.info("Report index not empty. Skipping indexing and in-db compression...");
            return;
        }
        LOGGER.info("Start indexing and in-db compression...");
        PageRequest firstPage = PageRequest.of(0, 10);
        int count = 0;
        migrate(firstPage, count);
    }

    private void migrate(Pageable pageable, int previousCount) {
        LOGGER.debug("Indexing and compressing reports in page n° {}", pageable.getPageNumber());
        Slice<ScenarioExecutionReportEntity> slice = scenarioExecutionReportJpaRepository.findAll(pageable);
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
        scenarioExecutionReportIndexRepository.saveAll(reportsInDb);
    }

    private boolean isMigrationDone() {
        int indexedReports = scenarioExecutionReportIndexRepository.count();
        return indexedReports > 0;
    }
}
