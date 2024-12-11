/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.migration.domain;

import com.chutneytesting.campaign.infra.CampaignJpaRepository;
import com.chutneytesting.campaign.infra.index.CampaignIndexRepository;
import com.chutneytesting.campaign.infra.jpa.CampaignEntity;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class CampaignMigrator implements DataMigrator {

    private final CampaignJpaRepository campaignJpaRepository;
    private final CampaignIndexRepository campaignIndexRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignMigrator.class);

    public CampaignMigrator(CampaignJpaRepository campaignJpaRepository, CampaignIndexRepository campaignIndexRepository) {
        this.campaignJpaRepository = campaignJpaRepository;
        this.campaignIndexRepository = campaignIndexRepository;
    }


    @Override
    public void migrate() {
        if (isMigrationDone()) {
            LOGGER.info("Campaigns index not empty. Skipping indexing...");
            return;
        }
        LOGGER.info("Start indexing...");
        PageRequest firstPage = PageRequest.of(0, 10);
        int count = 0;
        migrate(firstPage, count);
    }

    private void migrate(Pageable pageable, int previousCount) {
        LOGGER.debug("Indexing in page n° {}", pageable.getPageNumber());
        Slice<CampaignEntity> slice = campaignJpaRepository.findAll(pageable);
        List<CampaignEntity> campaigns = slice.getContent();
        index(campaigns);
        int count = previousCount + slice.getNumberOfElements();
        if (slice.hasNext()) {
            migrate(slice.nextPageable(), count);
        } else {
            LOGGER.info("{} campaigns(s) successfully compressed and indexed", count);
        }
    }

    private void index(List<CampaignEntity> campaigns) {
        campaignIndexRepository.saveAll(campaigns);
    }

    private boolean isMigrationDone() {
        int indexedReports = campaignIndexRepository.count();
        return indexedReports > 0;
    }
}
