/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import fr.enedis.chutney.campaign.infra.jpa.CampaignScenarioEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface CampaignScenarioJpaRepository extends CrudRepository<CampaignScenarioEntity, Long>, JpaSpecificationExecutor<CampaignScenarioEntity> {

    List<CampaignScenarioEntity> findAllByScenarioId(String scenarioId);
    List<CampaignScenarioEntity> findAllByDatasetId(String datasetId);
}
