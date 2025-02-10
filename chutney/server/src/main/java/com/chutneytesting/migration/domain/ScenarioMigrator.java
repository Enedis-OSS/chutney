package com.chutneytesting.migration.domain;

import com.chutneytesting.scenario.infra.index.ScenarioIndexRepository;
import com.chutneytesting.scenario.infra.jpa.ScenarioEntity;
import com.chutneytesting.scenario.infra.raw.ScenarioJpaRepository;
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
