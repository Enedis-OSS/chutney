/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

/**
 * To be caught by <b>spring</b> fault-barrier and processed by <b>spring-boot</b> error handler.
 */
@SuppressWarnings("serial")
public class ScheduledCampaignNotFoundException extends RuntimeException {
    public static final String NOT_FOUND_MESSAGE = "Given ID does not match any scheduled campaign";

    public ScheduledCampaignNotFoundException(Long scheduledCampaignId) {
        super(NOT_FOUND_MESSAGE + ": scheduledCampaignId=" + scheduledCampaignId);
    }
}
