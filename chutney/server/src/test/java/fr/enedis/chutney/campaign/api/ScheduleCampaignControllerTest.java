/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.enedis.chutney.campaign.domain.Frequency;
import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign;
import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign.CampaignExecutionRequest;
import fr.enedis.chutney.campaign.domain.ScheduledCampaignRepository;
import fr.enedis.chutney.config.web.RestExceptionHandler;
import fr.enedis.chutney.config.web.WebConfiguration;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class ScheduleCampaignControllerTest {

    private MockMvc mockMvc;

    private final JsonMapper objectMapper = (JsonMapper) new WebConfiguration().webObjectMapper();

    private final ScheduledCampaignRepository scheduledCampaignRepository = mock(ScheduledCampaignRepository.class);

    private final LocalDateTime now = LocalDateTime.of(2024, 10, 20, 10, 0);
    private final Clock clock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private final ScheduleCampaignController scheduleCampaignController = new ScheduleCampaignController(scheduledCampaignRepository, clock);

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(scheduleCampaignController)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
            .setControllerAdvice(new RestExceptionHandler(Mockito.mock(ChutneyMetrics.class)))
            .build();
    }

    @Test
    @WithMockUser(authorities = "CAMPAIGN_READ")
    void should_get_all_scheduled_campaigns() throws Exception {

        CampaignExecutionRequest request = new CampaignExecutionRequest(1L, "title", "datasetId", "JIRA-5");
        when(scheduledCampaignRepository.getAll()).thenReturn(List.of(
            new PeriodicScheduledCampaign(1L, LocalDateTime.of(2024, 10, 12, 14, 30, 45), Frequency.DAILY, "PROD", List.of(request))
        ));

        // Act & Assert
        mockMvc.perform(get("/api/ui/campaign/v1/scheduling")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].frequency").value("Daily"))
            .andExpect(jsonPath("$[0].environment").value("PROD"))
            .andExpect(jsonPath("$[0].campaignExecutionRequest[0].campaignId").value(1L))
            .andExpect(jsonPath("$[0].campaignExecutionRequest[0].campaignTitle").value("title"))
            .andExpect(jsonPath("$[0].campaignExecutionRequest[0].datasetId").value("datasetId"))
            .andExpect(jsonPath("$[0].campaignExecutionRequest[0].jiraId").value("JIRA-5"))
            .andExpect(jsonPath("$[0].schedulingDate").value("2024-10-12T14:30:45"));

        verify(scheduledCampaignRepository).getAll();
    }

    @Test
    @WithMockUser(authorities = "CAMPAIGN_WRITE")
    void should_add_scheduled_campaign() throws Exception {

        mockMvc.perform(post("/api/ui/campaign/v1/scheduling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {
                                "id":1,
                                "schedulingDate":"2024-10-12T14:30:45",
                                "frequency":"Daily",
                                "environment":"PROD",
                                "campaignExecutionRequest":[
                                    {"campaignId":1,"campaignTitle":"title","datasetId":"datasetId", "jiraId":"JIRA-67"}
                                ]
                            }
                        """))
            .andExpect(status().isOk());

        ArgumentCaptor<PeriodicScheduledCampaign> captor = ArgumentCaptor.forClass(PeriodicScheduledCampaign.class);

        verify(scheduledCampaignRepository).add(captor.capture());

        PeriodicScheduledCampaign capturedCampaign = captor.getValue();

        assertThat(capturedCampaign)
            .isNotNull()
            .extracting("id", "nextExecutionDate", "frequency.label", "environment")
            .containsExactly(
                1L,
                LocalDateTime.of(2024, 10, 12, 14, 30, 45),
                "Daily",
                "PROD"
            );

        assertThat(capturedCampaign.campaignExecutionRequests)
            .hasSize(1)
            .first()
            .extracting("campaignId", "campaignTitle", "datasetId", "jiraId")
            .containsExactly(1L, "title", "datasetId", "JIRA-67");
    }

    @Test
    @WithMockUser(authorities = "CAMPAIGN_WRITE")
    void should_delete_scheduled_campaign() throws Exception {
        Long schedulingCampaignId = 1L;

        mockMvc.perform(delete("/api/ui/campaign/v1/scheduling/{schedulingCampaignId}", schedulingCampaignId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(scheduledCampaignRepository).removeById(schedulingCampaignId);
    }

    @Test
    @WithMockUser(authorities = "CAMPAIGN_WRITE")
    void should_disable_scheduled_campaign() throws Exception {
        CampaignExecutionRequest request = new CampaignExecutionRequest(1L, "title", "", "");
        when(scheduledCampaignRepository.findById(1L)).thenReturn(Optional.of(
            new PeriodicScheduledCampaign(1L, LocalDateTime.of(2024, 10, 21, 8, 0), Frequency.DAILY, "PROD", List.of(request))
        ));

        mockMvc.perform(post("/api/ui/campaign/v1/scheduling/{schedulingCampaignId}/disable", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.schedulingDate").value("2024-10-21T08:00:00"));

        ArgumentCaptor<PeriodicScheduledCampaign> captor = ArgumentCaptor.forClass(PeriodicScheduledCampaign.class);
        verify(scheduledCampaignRepository).update(captor.capture());
        assertThat(captor.getValue().enabled).isFalse();
    }

    @Test
    @WithMockUser(authorities = "CAMPAIGN_WRITE")
    void should_enable_scheduled_campaign_on_its_next_execution() throws Exception {
        CampaignExecutionRequest request = new CampaignExecutionRequest(1L, "title", "", "");
        when(scheduledCampaignRepository.findById(1L)).thenReturn(Optional.of(
            new PeriodicScheduledCampaign(1L, LocalDateTime.of(2024, 10, 12, 8, 0), Frequency.DAILY, "PROD", List.of(request), false)
        ));

        mockMvc.perform(post("/api/ui/campaign/v1/scheduling/{schedulingCampaignId}/enable", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.schedulingDate").value("2024-10-21T08:00:00"));

        ArgumentCaptor<PeriodicScheduledCampaign> captor = ArgumentCaptor.forClass(PeriodicScheduledCampaign.class);
        verify(scheduledCampaignRepository).update(captor.capture());
        assertThat(captor.getValue().enabled).isTrue();
        assertThat(captor.getValue().nextExecutionDate).isEqualTo(LocalDateTime.of(2024, 10, 21, 8, 0));
    }

    @Test
    @WithMockUser(authorities = "CAMPAIGN_WRITE")
    void should_return_not_found_when_scheduled_campaign_does_not_exist() throws Exception {
        when(scheduledCampaignRepository.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/ui/campaign/v1/scheduling/{schedulingCampaignId}/disable", 42L))
            .andExpect(status().isNotFound());
    }
}
