/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class CampaignEnvironmentUpdateHandlerTest {

    @Test
    void should_rename_environment_in_campaign() {
        // Given
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignEnvironmentUpdateHandler sut = new CampaignEnvironmentUpdateHandler(campaignRepository);
        Campaign campaign1 = new Campaign(
            1L,
            "TITLE1",
            "DESCRIPTION1",
            List.of(),
            "ENV",
            false,
            false,
            "DATASET1",
            List.of()
        );
        Campaign campaign2 = new Campaign(
            2L,
            "TITLE2",
            "DESCRIPTION2",
            List.of(),
            "ENV",
            false,
            false,
            "DATASET2",
            List.of()
        );

        ArgumentCaptor<Campaign> argument = ArgumentCaptor.forClass(Campaign.class);
        when(campaignRepository.findCampaignsByEnvironment("ENV")).thenReturn(List.of(campaign1, campaign2));
        when(campaignRepository.createOrUpdate(any(Campaign.class))).thenReturn(any(Campaign.class));


        // When
        sut.renameEnvironment("ENV", "NEW_ENV");

        // Then
        verify(campaignRepository, times(2)).createOrUpdate(argument.capture());
        List<Campaign> campaigns = argument.getAllValues();
        assertThat(campaigns).hasSize(2);
        assertThat("TITLE1").isEqualTo(campaigns.getFirst().title);
        assertThat("NEW_ENV").isEqualTo(campaigns.getFirst().executionEnvironment());
        assertThat("TITLE2").isEqualTo(campaigns.get(1).title);
        assertThat("NEW_ENV").isEqualTo(campaigns.get(1).executionEnvironment());
    }

    @Test
    void should_delete_environment_in_campaign() {
        // Given
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignEnvironmentUpdateHandler sut = new CampaignEnvironmentUpdateHandler(campaignRepository);
        Campaign campaign1 = new Campaign(
            1L,
            "TITLE1",
            "DESCRIPTION1",
            List.of(),
            "ENV",
            false,
            false,
            "DATASET1",
            List.of()
        );
        Campaign campaign2 = new Campaign(
            2L,
            "TITLE2",
            "DESCRIPTION2",
            List.of(),
            "ENV",
            false,
            false,
            "DATASET2",
            List.of()
        );

        ArgumentCaptor<Campaign> argument = ArgumentCaptor.forClass(Campaign.class);
        when(campaignRepository.findCampaignsByEnvironment("ENV")).thenReturn(List.of(campaign1, campaign2));
        when(campaignRepository.createOrUpdate(any(Campaign.class))).thenReturn(any(Campaign.class));


        // When
        sut.deleteEnvironment("ENV");

        // Then
        verify(campaignRepository, times(2)).createOrUpdate(argument.capture());
        List<Campaign> campaigns = argument.getAllValues();
        assertThat(campaigns).hasSize(2);
        assertThat("TITLE1").isEqualTo(campaigns.getFirst().title);
        assertThat(campaigns.getFirst().executionEnvironment()).isNull();
        assertThat("TITLE2").isEqualTo(campaigns.get(1).title);
        assertThat(campaigns.get(1).executionEnvironment()).isNull();
    }
}
