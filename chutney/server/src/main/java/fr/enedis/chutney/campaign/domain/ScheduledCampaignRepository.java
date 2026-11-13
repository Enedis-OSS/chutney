/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

import java.util.List;
import java.util.Optional;

/**
 * CRUD for SchedulingCampaign
 */
public interface ScheduledCampaignRepository {

    PeriodicScheduledCampaign add(PeriodicScheduledCampaign periodicScheduledCampaign);

    Optional<PeriodicScheduledCampaign> findById(Long id);

    void update(PeriodicScheduledCampaign periodicScheduledCampaign);

    void removeById(Long id);

    void removeCampaignId(Long id);

    List<PeriodicScheduledCampaign> getAll();
}
