/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PeriodicScheduledCampaign {

    public final Long id;
    public final LocalDateTime nextExecutionDate;
    public final Frequency frequency;
    public final String environment;
    public final List<CampaignExecutionRequest> campaignExecutionRequests;
    public final boolean enabled;

    public PeriodicScheduledCampaign(Long id, LocalDateTime nextExecutionDate, Frequency frequency, String environment, List<CampaignExecutionRequest> campaignExecutionRequests) {
        this(id, nextExecutionDate, frequency, environment, campaignExecutionRequests, true);
    }

    public PeriodicScheduledCampaign(Long id, LocalDateTime nextExecutionDate, Frequency frequency, String environment, List<CampaignExecutionRequest> campaignExecutionRequests, boolean enabled) {
        this.id = id;
        this.nextExecutionDate = nextExecutionDate;
        this.frequency = frequency;
        this.environment = environment;
        this.campaignExecutionRequests = campaignExecutionRequests;
        this.enabled = enabled;
    }

    public PeriodicScheduledCampaign nextScheduledExecution() {
        LocalDateTime scheduledDate = switch (this.frequency) {
            case HOURLY -> this.nextExecutionDate.plusHours(1);
            case DAILY -> this.nextExecutionDate.plusDays(1);
            case WEEKLY -> this.nextExecutionDate.plusWeeks(1);
            case MONTHLY -> this.nextExecutionDate.plusMonths(1);
            default -> throw new IllegalStateException("Unexpected value: " + this.frequency);
        };
        return new PeriodicScheduledCampaign(id, scheduledDate, frequency, environment, campaignExecutionRequests, enabled);
    }

    public PeriodicScheduledCampaign disable() {
        if (Frequency.EMPTY.equals(frequency)) {
            throw new IllegalArgumentException("Cannot disable a scheduled campaign without frequency: id=" + id);
        }
        return new PeriodicScheduledCampaign(id, nextExecutionDate, frequency, environment, campaignExecutionRequests, false);
    }

    public PeriodicScheduledCampaign enable(LocalDateTime now) {
        if (enabled) {
            return this;
        }
        PeriodicScheduledCampaign enabledCampaign = new PeriodicScheduledCampaign(id, nextExecutionDate, frequency, environment, campaignExecutionRequests, true);
        while (enabledCampaign.nextExecutionDate.isBefore(now)) {
            enabledCampaign = enabledCampaign.nextScheduledExecution();
        }
        return enabledCampaign;
    }

    public record CampaignExecutionRequest(Long campaignId, String campaignTitle, String datasetId, String jiraId) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodicScheduledCampaign that = (PeriodicScheduledCampaign) o;
        return enabled == that.enabled &&
            Objects.equals(id, that.id) &&
            Objects.equals(nextExecutionDate, that.nextExecutionDate) &&
            Objects.equals(frequency, that.frequency) &&
            Objects.equals(environment, that.environment) &&
            Objects.equals(campaignExecutionRequests, that.campaignExecutionRequests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nextExecutionDate, frequency, environment, campaignExecutionRequests, enabled);
    }

    @Override
    public String toString() {
        return "SchedulingCampaign{" +
            "id=" + id +
            ", schedulingDate=" + nextExecutionDate +
            ", frequency='" + frequency + '\'' +
            ", environment='" + environment + '\'' +
            ", campaignExecutionRequests='" + campaignExecutionRequests + '\'' +
            ", enabled=" + enabled +
            '}';
    }
}
