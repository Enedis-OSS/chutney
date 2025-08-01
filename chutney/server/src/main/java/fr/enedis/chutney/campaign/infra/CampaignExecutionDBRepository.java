/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.domain.CampaignNotFoundException;
import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.campaign.infra.jpa.CampaignExecutionEntity;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionNotFoundException;
import fr.enedis.chutney.execution.infra.storage.DatabaseExecutionJpaRepository;
import fr.enedis.chutney.execution.infra.storage.jpa.ScenarioExecutionEntity;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CampaignExecutionDBRepository implements CampaignExecutionRepository {

    private final CampaignExecutionJpaRepository campaignExecutionJpaRepository;
    private final CampaignJpaRepository campaignJpaRepository;
    private final DatabaseExecutionJpaRepository scenarioExecutionJpaRepository;
    private final Map<Long, List<CampaignExecution>> currentCampaignExecutions = new ConcurrentHashMap<>();

    public CampaignExecutionDBRepository(
        CampaignExecutionJpaRepository campaignExecutionJpaRepository,
        CampaignJpaRepository campaignJpaRepository,
        DatabaseExecutionJpaRepository scenarioExecutionJpaRepository
    ) {
        this.campaignExecutionJpaRepository = campaignExecutionJpaRepository;
        this.campaignJpaRepository = campaignJpaRepository;
        this.scenarioExecutionJpaRepository = scenarioExecutionJpaRepository;
    }

    @Override
    public List<CampaignExecution> currentExecutions(Long campaignId) {
        return currentCampaignExecutions.getOrDefault(campaignId, emptyList());
    }

    @Override
    public void startExecution(Long campaignId, CampaignExecution campaignExecution) {
        List<CampaignExecution> campaignExecutions = new ArrayList<>();
        if (currentCampaignExecutions.containsKey(campaignId)) {
            campaignExecutions = currentCampaignExecutions.get(campaignId);
        }
        campaignExecutions.add(campaignExecution);
        currentCampaignExecutions.put(campaignId, campaignExecutions);
    }

    @Override
    public void stopExecution(Long campaignId, String environment) {
        currentCampaignExecutions.get(campaignId)
            .removeIf(exec -> exec.executionEnvironment.equals(environment));
        if (currentCampaignExecutions.get(campaignId).isEmpty()) {
            currentCampaignExecutions.remove(campaignId);
        }
    }

    @Override
    public CampaignExecution getLastExecution(Long campaignId) {
        return campaignExecutionJpaRepository
            .findFirstByCampaignIdOrderByIdDesc(campaignId)
            .map(this::toDomain)
            .orElseThrow(() -> new CampaignExecutionNotFoundException(campaignId));
    }

    @Override
    @Transactional
    public void deleteExecutions(Set<Long> executionsIds) {
        List<CampaignExecutionEntity> executions = campaignExecutionJpaRepository.findAllById(executionsIds);
        List<ScenarioExecutionEntity> scenarioExecutions = executions.stream().flatMap(cer -> cer.scenarioExecutions().stream()).toList();
        scenarioExecutions.forEach(ScenarioExecutionEntity::clearCampaignExecution);
        scenarioExecutionJpaRepository.saveAll(scenarioExecutions);
        campaignExecutionJpaRepository.deleteAllInBatch(executions);
    }

    @Override
    public List<CampaignExecution> getExecutionHistory(Long campaignId) {
        return campaignExecutionJpaRepository.findByCampaignIdOrderByIdDesc(campaignId).stream()
            .map(this::toDomain)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void saveCampaignExecution(Long campaignId, CampaignExecution campaignExecution) {
        CampaignExecutionEntity execution = campaignExecutionJpaRepository.findById(campaignExecution.executionId).orElseThrow(
            () -> new CampaignExecutionNotFoundException(campaignId, campaignExecution.executionId)
        );
        Iterable<ScenarioExecutionEntity> scenarioExecutions =
            scenarioExecutionJpaRepository.findAllById(campaignExecution.scenarioExecutionReports().stream()
                .map(serc -> serc.execution().executionId())
                .toList());
        execution.updateFromDomain(campaignExecution, scenarioExecutions);
        campaignExecutionJpaRepository.save(execution);
    }

    @Override
    public List<CampaignExecution> getLastExecutions(Long numberOfExecution) {
        return campaignExecutionJpaRepository.findAll(
                PageRequest.of(0, numberOfExecution.intValue(), Sort.by(Sort.Direction.DESC, "id"))).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public CampaignExecution getCampaignExecutionById(Long campaignExecId) {
        return campaignExecutionJpaRepository.findById(campaignExecId)
            .map(this::toDomain)
            .orElseThrow(() -> new CampaignExecutionNotFoundException(null, campaignExecId));
    }

    @Override
    @Transactional
    public void clearAllExecutionHistory(Long campaignId) {
        List<CampaignExecutionEntity> campaignExecutionEntities = campaignExecutionJpaRepository.findAllByCampaignId(campaignId);
        List<ScenarioExecutionEntity> scenarioExecutions = campaignExecutionEntities.stream().flatMap(ce -> ce.scenarioExecutions().stream()).toList();
        scenarioExecutions.forEach(ScenarioExecutionEntity::clearCampaignExecution);
        scenarioExecutionJpaRepository.saveAll(scenarioExecutions);
        campaignExecutionJpaRepository.deleteAllInBatch(campaignExecutionEntities);
    }

    @Override
    @Transactional
    public Long generateCampaignExecutionId(Long campaignId, String environment, DataSet dataset) {
        notNull(campaignId, "Campaign ID cannot be null");
        notBlank(environment, "Environment cannot be null or empty");

        CampaignExecutionEntity newExecution = new CampaignExecutionEntity(campaignId, environment, dataset);
        campaignExecutionJpaRepository.save(newExecution);
        return newExecution.id();
    }

    private CampaignExecution toDomain(CampaignExecutionEntity campaignExecution) {
        CampaignEntity campaign = campaignJpaRepository.findById(campaignExecution.campaignId())
            .orElseThrow(() -> new CampaignNotFoundException(campaignExecution.campaignId()));
        return ofNullable(runningCampaignExecution(campaignExecution)).orElseGet(() ->
            campaignExecution.toDomain(campaign.title())
        );
    }

    private CampaignExecution runningCampaignExecution(CampaignExecutionEntity campaignExecutionEntity) {
        return currentExecutions(campaignExecutionEntity.campaignId())
            .stream()
            .filter(exec -> exec.executionId.equals(campaignExecutionEntity.id()))
            .findAny()
            .orElse(null);
    }
}
