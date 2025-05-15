/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.migration.domain;

import fr.enedis.chutney.scenario.infra.index.ScenarioIndexRepository;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.scenario.infra.raw.ScenarioJpaRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class ScenarioMigrator extends AbstractMigrator<ScenarioEntity> {

    private final ScenarioJpaRepository scenarioJpaRepository;
    private final ScenarioIndexRepository scenarioIndexRepository;

    public ScenarioMigrator(ScenarioJpaRepository scenarioJpaRepository, ScenarioIndexRepository scenarioIndexRepository) {
        this.scenarioJpaRepository = scenarioJpaRepository;
        this.scenarioIndexRepository = scenarioIndexRepository;
    }

    @Override
    protected Slice<ScenarioEntity> findAll(Pageable pageable) {
        return scenarioJpaRepository.findByActivated(true, pageable);
    }

    @Override
    protected void index(List<ScenarioEntity> entities) {
        scenarioIndexRepository.saveAll(entities);
    }

    @Override
    protected boolean isMigrationDone() {
        return scenarioIndexRepository.count() > 0;
    }

    @Override
    protected String getEntityName() {
        return "scenario";
    }
}
