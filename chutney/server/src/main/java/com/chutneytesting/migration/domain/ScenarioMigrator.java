/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.migration.domain;

import com.chutneytesting.scenario.infra.index.ScenarioIndexRepository;
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
public class ScenarioMigrator implements DataMigrator {

    private final ScenarioJpaRepository scenarioJpaRepository;
    private final ScenarioIndexRepository scenarioIndexRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioMigrator.class);

    public ScenarioMigrator(ScenarioJpaRepository scenarioJpaRepository, ScenarioIndexRepository scenarioIndexRepository) {
        this.scenarioJpaRepository = scenarioJpaRepository;
        this.scenarioIndexRepository = scenarioIndexRepository;
    }


    @Override
    public void migrate() {
        if (isMigrationDone()) {
            LOGGER.info("Scenarios index not empty. Skipping indexing...");
            return;
        }
        LOGGER.info("Start indexing...");
        PageRequest firstPage = PageRequest.of(0, 10);
        int count = 0;
        migrate(firstPage, count);
    }

    private void migrate(Pageable pageable, int previousCount) {
        LOGGER.debug("Indexing page n° {}", pageable.getPageNumber());
        Slice<ScenarioEntity> slice = scenarioJpaRepository.findByActivated(true,pageable);
        List<ScenarioEntity> scenarios = slice.getContent();
        index(scenarios);
        int count = previousCount + slice.getNumberOfElements();
        if (slice.hasNext()) {
            migrate(slice.nextPageable(), count);
        } else {
            LOGGER.info("{} scenario(s) successfully indexed", count);
        }
    }

    private void index(List<ScenarioEntity> campaigns) {
        scenarioIndexRepository.saveAll(campaigns);
    }

    private boolean isMigrationDone() {
        int indexedReports = scenarioIndexRepository.count();
        return indexedReports > 0;
    }
}
