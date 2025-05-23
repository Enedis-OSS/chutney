/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.campaign.domain.CampaignExecutionRepository;
import fr.enedis.chutney.campaign.domain.CampaignNotFoundException;
import fr.enedis.chutney.campaign.domain.CampaignRepository;
import fr.enedis.chutney.campaign.domain.ScheduledCampaignRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.campaign.infra.jpa.CampaignScenarioEntity;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campaign persistence management.
 */
@Repository
@Transactional(readOnly = true)
public class DatabaseCampaignRepository implements CampaignRepository {

    private final CampaignJpaRepository campaignJpaRepository;
    private final CampaignScenarioJpaRepository campaignScenarioJpaRepository;
    private final CampaignExecutionRepository campaignExecutionRepository;
    private final ScheduledCampaignRepository scheduledCampaignRepository;

    public DatabaseCampaignRepository(CampaignJpaRepository campaignJpaRepository,
                                      CampaignScenarioJpaRepository campaignScenarioJpaRepository,
                                      CampaignExecutionDBRepository campaignExecutionRepository, ScheduledCampaignRepository scheduledCampaignRepository) {
        this.campaignJpaRepository = campaignJpaRepository;
        this.campaignScenarioJpaRepository = campaignScenarioJpaRepository;
        this.campaignExecutionRepository = campaignExecutionRepository;
        this.scheduledCampaignRepository = scheduledCampaignRepository;
    }

    @Override
    @Transactional
    public Campaign createOrUpdate(Campaign campaign) {
        if (campaign.id != null && !campaignJpaRepository.existsById(campaign.id)) {
            CampaignEntity campaignEntity = CampaignEntity.fromDomain(campaign, 1);
            campaignJpaRepository.saveWithExplicitId(campaignEntity.id(), campaignEntity.title(), campaignEntity.description());
        }
        CampaignEntity campaignJpa =
            campaignJpaRepository.save(CampaignEntity.fromDomain(campaign, lastCampaignVersion(campaign.id)));
        return campaignJpa.toDomain();
    }

    private Integer lastCampaignVersion(Long id) {
        return ofNullable(id).flatMap(campaignJpaRepository::findById).map(CampaignEntity::version).orElse(null);
    }

    @Override
    @Transactional
    public boolean removeById(Long id) {
        if (campaignJpaRepository.existsById(id)) {
            campaignExecutionRepository.clearAllExecutionHistory(id);
            campaignJpaRepository.deleteById(id);
            scheduledCampaignRepository.removeCampaignId(id);
            return true;
        }
        return false;
    }

    @Override
    public Campaign findById(Long campaignId) throws CampaignNotFoundException {
        return campaignJpaRepository.findById(campaignId)
            .map(CampaignEntity::toDomain)
            .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    }

    @Override
    public List<Campaign> findByName(String campaignName) {
        return campaignJpaRepository.findAll((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), campaignName.toLowerCase()))
            .stream()
            .map(CampaignEntity::toDomain)
            .toList();
    }

    @Override
    public List<String> findScenariosIds(Long campaignId) {
        return campaignJpaRepository.findById(campaignId)
            .map(c -> c.campaignScenarios().stream()
                .map(CampaignScenarioEntity::scenarioId)
                .toList()
            )
            .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    }

    @Override
    public List<Campaign> findAll() {
        return StreamSupport.stream(campaignJpaRepository.findAll().spliterator(), false)
            .map(CampaignEntity::toDomain)
            .toList();
    }

    @Override
    public List<Campaign> findCampaignsByScenarioId(String scenarioId) {
        if (isNullOrEmpty(scenarioId)) {
            return emptyList();
        }

        return campaignScenarioJpaRepository.findAllByScenarioId(scenarioId).stream()
            .map(CampaignScenarioEntity::campaign)
            .distinct()
            .map(CampaignEntity::toDomain)
            .toList();
    }

    @Override
    public List<Campaign> findCampaignsByEnvironment(String environment) {
        if (isNullOrEmpty(environment)) {
            return emptyList();
        }
        return campaignJpaRepository.findByEnvironment(environment).stream()
            .map(CampaignEntity::toDomain)
            .toList();
    }

    @Override
    public List<Campaign> findCampaignsByDatasetId(String datasetId) {
        if (isNullOrEmpty(datasetId)) {
            return emptyList();
        }
        return campaignJpaRepository.findByDatasetId(datasetId).stream()
            .map(CampaignEntity::toDomain)
            .toList();
    }
}
