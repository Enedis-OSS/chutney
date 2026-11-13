/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign.CampaignExecutionRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PeriodicScheduledCampaignTest {

    private final LocalDateTime saturday = LocalDateTime.of(2026, 11, 14, 8, 0);
    private final List<CampaignExecutionRequest> requests = List.of(new CampaignExecutionRequest(1L, "title", "", ""));

    @Test
    void should_disable_scheduled_campaign() {
        PeriodicScheduledCampaign sc = new PeriodicScheduledCampaign(1L, saturday, Frequency.WEEKLY, "ENV", requests);

        PeriodicScheduledCampaign disabled = sc.disable();

        assertThat(disabled.enabled).isFalse();
        assertThat(disabled.nextExecutionDate).isEqualTo(saturday);
    }

    @Test
    void should_not_disable_scheduled_campaign_without_frequency() {
        PeriodicScheduledCampaign sc = new PeriodicScheduledCampaign(1L, saturday, Frequency.EMPTY, "ENV", requests);

        assertThatThrownBy(sc::disable).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "HOURLY, 2026-11-16T10:30, 2026-11-16T11:00",
        "DAILY, 2026-11-16T10:00, 2026-11-17T08:00",
        "WEEKLY, 2026-11-16T10:00, 2026-11-21T08:00",
        "MONTHLY, 2026-11-16T10:00, 2026-12-14T08:00"
    })
    void should_enable_scheduled_campaign_on_its_next_execution(Frequency frequency, LocalDateTime now, LocalDateTime expectedNextExecution) {
        PeriodicScheduledCampaign sc = new PeriodicScheduledCampaign(1L, saturday, frequency, "ENV", requests, false);

        PeriodicScheduledCampaign enabled = sc.enable(now);

        assertThat(enabled.enabled).isTrue();
        assertThat(enabled.nextExecutionDate).isEqualTo(expectedNextExecution);
    }

    @Test
    void should_keep_next_execution_when_enabled_before_it() {
        PeriodicScheduledCampaign sc = new PeriodicScheduledCampaign(1L, saturday, Frequency.WEEKLY, "ENV", requests, false);

        PeriodicScheduledCampaign enabled = sc.enable(saturday.minusHours(1));

        assertThat(enabled.enabled).isTrue();
        assertThat(enabled.nextExecutionDate).isEqualTo(saturday);
    }

    @Test
    void should_not_change_already_enabled_scheduled_campaign() {
        PeriodicScheduledCampaign sc = new PeriodicScheduledCampaign(1L, saturday, Frequency.DAILY, "ENV", requests);

        assertThat(sc.enable(saturday.plusDays(2))).isEqualTo(sc);
    }
}
