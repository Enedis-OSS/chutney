/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package fr.enedis.chutney.migration.domain;

import fr.enedis.chutney.campaign.infra.CampaignJpaRepository;
import fr.enedis.chutney.campaign.infra.index.CampaignIndexRepository;
import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class CampaignMigrator extends AbstractMigrator<CampaignEntity> {

    private final CampaignJpaRepository campaignJpaRepository;
    private final CampaignIndexRepository campaignIndexRepository;

    public CampaignMigrator(CampaignJpaRepository campaignJpaRepository, CampaignIndexRepository campaignIndexRepository) {
        this.campaignJpaRepository = campaignJpaRepository;
        this.campaignIndexRepository = campaignIndexRepository;
    }

    @Override
    protected Slice<CampaignEntity> findAll(Pageable pageable) {
        return campaignJpaRepository.findAll(pageable);
    }

    @Override
    protected void index(List<CampaignEntity> entities) {
        campaignIndexRepository.saveAll(entities);
    }

    @Override
    protected boolean isMigrationDone() {
        return campaignIndexRepository.count() > 0;
    }

    @Override
    protected String getEntityName() {
        return "campaign";
    }
}
