/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.chutneytesting.migration.domain;

import com.chutneytesting.dataset.infra.FileDatasetRepository;
import com.chutneytesting.dataset.infra.index.DatasetIndexRepository;
import com.chutneytesting.server.core.domain.dataset.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatasetMigrator implements DataMigrator {

    private final FileDatasetRepository datasetRepository;
    private final DatasetIndexRepository datasetIndexRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetMigrator.class);

    public DatasetMigrator(FileDatasetRepository datasetRepository, DatasetIndexRepository datasetIndexRepository) {
        this.datasetRepository = datasetRepository;
        this.datasetIndexRepository = datasetIndexRepository;
    }

    @Override
    public void migrate() {
        if (isMigrationDone()) {
            LOGGER.info("Dataset index not empty. Skipping indexing...");
            return;
        }
        LOGGER.info("Start indexing...");
        List<DataSet> dataSets = datasetRepository.findAll();
        index(dataSets);
        LOGGER.info("{} dataset(s) successfully indexed", dataSets.size());
    }

    private void index(List<DataSet> dataSets) {
        datasetIndexRepository.saveAll(dataSets);
    }

    private boolean isMigrationDone() {
        int indexedReports = datasetIndexRepository.count();
        return indexedReports > 0;
    }
}
