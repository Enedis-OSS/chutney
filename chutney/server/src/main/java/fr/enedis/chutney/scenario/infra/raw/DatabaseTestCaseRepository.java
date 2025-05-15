/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.infra.raw;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Long.valueOf;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import fr.enedis.chutney.campaign.infra.CampaignScenarioJpaRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignScenarioEntity;
import fr.enedis.chutney.execution.infra.storage.DatabaseExecutionJpaRepository;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotParsableException;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DatabaseTestCaseRepository implements AggregatedRepository<GwtTestCase> {

    private final ScenarioJpaRepository scenarioJpaRepository;
    private final DatabaseExecutionJpaRepository scenarioExecutionsJpaRepository;
    private final CampaignScenarioJpaRepository campaignScenarioJpaRepository;


    public DatabaseTestCaseRepository(
        ScenarioJpaRepository jpa,
        DatabaseExecutionJpaRepository scenarioExecutionsJpaRepository,
        CampaignScenarioJpaRepository campaignScenarioJpaRepository, EntityManager entityManager) {
        this.scenarioJpaRepository = jpa;
        this.scenarioExecutionsJpaRepository = scenarioExecutionsJpaRepository;
        this.campaignScenarioJpaRepository = campaignScenarioJpaRepository;
    }

    @Override
    @Transactional
    public String save(GwtTestCase testCase) {
        if (scenarioWithExplicitIdNotExists(testCase)) {
            saveScenarioWithExplicitId(testCase);
            return testCase.id();
        }
        try {
            return scenarioJpaRepository.save(ScenarioEntity.fromGwtTestCase(testCase)).getId().toString();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ScenarioNotFoundException(testCase.id(), testCase.metadata().version());
        }
    }

    private boolean scenarioWithExplicitIdNotExists(TestCase testCase) {
        var testCaseId = testCase.id();
        try {
            return testCaseId != null && !scenarioJpaRepository.existsById(Long.parseLong(testCaseId));
        } catch (NumberFormatException e) {
            throw new ScenarioNotParsableException("Cannot parse id", e);
        }
    }

    @Override
    public Optional<GwtTestCase> findById(String scenarioId) {
        if (checkIdInput(scenarioId)) {
            return empty();
        }
        Optional<ScenarioEntity> scenarioDao = scenarioJpaRepository.findByIdAndActivated(valueOf(scenarioId), true)
            .filter(ScenarioEntity::isActivated);
        return scenarioDao.map(ScenarioEntity::toGwtTestCase);
    }

    @Override
    public Optional<TestCase> findExecutableById(String id) {
        return findById(id).map(TestCase.class::cast);
    }

    @Override
    public Optional<TestCaseMetadata> findMetadataById(String testCaseId) {
        if (checkIdInput(testCaseId)) {
            return empty();
        }
        return scenarioJpaRepository.findMetaDataByIdAndActivated(valueOf(testCaseId), true).map(ScenarioEntity::toTestCaseMetadata);
    }

    @Override
    public List<TestCaseMetadata> findAll() {
        return scenarioJpaRepository.findMetaDataByActivatedTrue().stream()
            .map(ScenarioEntity::toTestCaseMetadata)
            .toList();
    }

    @Override
    public List<TestCaseMetadata> findAllByDatasetId(String datasetId) {
        return scenarioJpaRepository.findByActivatedTrueAndDefaultDataset(datasetId).stream()
            .map(ScenarioEntity::toTestCaseMetadata)
            .toList();
    }

    @Override
    @Transactional
    public void removeById(String scenarioId) {
        if (checkIdInput(scenarioId)) {
            return;
        }
        scenarioJpaRepository.findByIdAndActivated(valueOf(scenarioId), true)
            .ifPresent(scenarioJpa -> {
                List<ScenarioExecutionEntity> allExecutions = scenarioExecutionsJpaRepository.findAllByScenarioId(scenarioId);
                allExecutions.forEach(e -> {
                    e.forCampaignExecution(null);
                    scenarioExecutionsJpaRepository.save(e);
                });

                List<CampaignScenarioEntity> allCampaignScenarioEntities = campaignScenarioJpaRepository.findAllByScenarioId(scenarioId);
                campaignScenarioJpaRepository.deleteAll(allCampaignScenarioEntities);

                scenarioJpa.deactivate();
                scenarioJpaRepository.save(scenarioJpa);
            });
    }

    @Override
    public Optional<Integer> lastVersion(String scenarioId) {
        if (checkIdInput(scenarioId)) {
            return empty();
        }
        try {
            return scenarioJpaRepository.lastVersion(valueOf(scenarioId));
        } catch (IncorrectResultSizeDataAccessException e) {
            return empty();
        }
    }

    private void saveScenarioWithExplicitId(GwtTestCase testCase) {
        ScenarioEntity scenarioEntity = ScenarioEntity.fromGwtTestCase(testCase);
        scenarioJpaRepository.saveWithExplicitId(
            scenarioEntity.getId(),
            scenarioEntity.getTitle(),
            scenarioEntity.getDescription(),
            scenarioEntity.getContent(),
            scenarioEntity.getTags(),
            scenarioEntity.getCreationDate(),
            scenarioEntity.isActivated(),
            scenarioEntity.getUserId(),
            scenarioEntity.getUpdateDate(),
            scenarioEntity.getVersion(),
            scenarioEntity.getDefaultDataset()
        );
    }

    private boolean checkIdInput(String scenarioId) {
        return isNullOrEmpty(scenarioId) || !isNumeric(scenarioId);
    }
}
