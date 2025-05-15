/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import fr.enedis.chutney.campaign.infra.jpa.CampaignExecutionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CampaignExecutionJpaRepository extends JpaRepository<CampaignExecutionEntity, Long>, JpaSpecificationExecutor<CampaignExecutionEntity> {

    List<CampaignExecutionEntity> findByCampaignIdOrderByIdDesc(Long campaignId);

    List<CampaignExecutionEntity> findAllByCampaignId(Long campaignId);

    Optional<CampaignExecutionEntity> findFirstByCampaignIdOrderByIdDesc(Long campaignId);
}
