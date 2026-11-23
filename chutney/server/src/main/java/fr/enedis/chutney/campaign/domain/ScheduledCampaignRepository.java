/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * CRUD for SchedulingCampaign
 */
public interface ScheduledCampaignRepository {

    PeriodicScheduledCampaign add(PeriodicScheduledCampaign periodicScheduledCampaign);

    /**
     * Atomically read, change and save the scheduled campaign with the given id.
     *
     * @throws ScheduledCampaignNotFoundException if no scheduled campaign has this id
     */
    PeriodicScheduledCampaign update(Long id, UnaryOperator<PeriodicScheduledCampaign> updater);

    void removeById(Long id);

    void removeCampaignId(Long id);

    List<PeriodicScheduledCampaign> getAll();
}
