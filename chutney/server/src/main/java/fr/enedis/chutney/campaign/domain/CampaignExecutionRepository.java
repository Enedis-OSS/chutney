/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.util.List;
import java.util.Set;

public interface CampaignExecutionRepository {
    List<CampaignExecution> currentExecutions(Long campaignId);

    void startExecution(Long campaignId, CampaignExecution campaignExecution);

    void stopExecution(Long campaignId, String environment);

    CampaignExecution getLastExecution(Long campaignId);

    void deleteExecutions(Set<Long> executionsIds);

    void saveCampaignExecution(Long campaignId, CampaignExecution execution);

    void clearAllExecutionHistory(Long id);

    List<CampaignExecution> getLastExecutions(Long numberOfExecution);

    default Long generateCampaignExecutionId(Long campaignId, String environment) {
        return generateCampaignExecutionId(campaignId, environment, null);
    }

    Long generateCampaignExecutionId(Long campaignId, String environment, DataSet dataset);

    List<CampaignExecution> getExecutionHistory(Long campaignId);

    CampaignExecution getCampaignExecutionById(Long campaignExecutionId);
}
