/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.campaign.infra.aop;

import com.chutneytesting.campaign.infra.index.CampaignIndexRepository;
import com.chutneytesting.campaign.infra.jpa.CampaignEntity;
import com.chutneytesting.server.core.domain.scenario.campaign.Campaign;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CampaignIndexingAspect {
    private final CampaignIndexRepository campaignIndexRepository;

    public CampaignIndexingAspect(CampaignIndexRepository campaignIndexRepository) {
        this.campaignIndexRepository = campaignIndexRepository;
    }


    @AfterReturning(
        pointcut = "execution(* com.chutneytesting.campaign.infra.DatabaseCampaignRepository.createOrUpdate(..)))",
        returning = "createdCampaign",
        argNames = "createdCampaign")
    public void index(Campaign createdCampaign) {
        campaignIndexRepository.save(CampaignEntity.fromDomain(createdCampaign, 1));
    }

    @After("execution(* com.chutneytesting.campaign.infra.DatabaseCampaignRepository.removeById(..)) && args(id)")
    public void delete(Long id) {
        campaignIndexRepository.delete(id.toString());

    }
}
