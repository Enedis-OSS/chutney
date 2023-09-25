package com.chutneytesting.campaign.infra;

import com.chutneytesting.campaign.infra.jpa.CampaignEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface CampaignJpaRepository extends CrudRepository<CampaignEntity, Long>, JpaSpecificationExecutor<CampaignEntity> {
}