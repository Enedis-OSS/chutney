/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CampaignJpaRepository extends JpaRepository<CampaignEntity, Long>, JpaSpecificationExecutor<CampaignEntity> {

    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO CAMPAIGN (ID, TITLE, DESCRIPTION) VALUES (:id, :title, :description)")
    void saveWithExplicitId(
        Long id,
        String title,
        String description);

    List<CampaignEntity> findByEnvironment(String environment);
    List<CampaignEntity> findByDatasetId(String dataset);
}
