/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.domain.schedule;

import static fr.enedis.chutney.campaign.domain.Frequency.EMPTY;
import static fr.enedis.chutney.execution.domain.schedule.CampaignScheduler.SCHEDULER_EXECUTE_USER;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static util.AssertTestUtils.softAssertVerifies;

import fr.enedis.chutney.campaign.domain.Frequency;
import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign;
import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign.CampaignExecutionRequest;
import fr.enedis.chutney.campaign.domain.ScheduledCampaignRepository;
import fr.enedis.chutney.execution.domain.campaign.CampaignExecutionEngine;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.VerificationCollector;

public class CampaignSchedulerTest {

    @Rule
    public final VerificationCollector collector = MockitoJUnit.collector();

    private CampaignScheduler sut;
    private final CampaignExecutionEngine campaignExecutionEngine = mock(CampaignExecutionEngine.class);
    private final ScheduledCampaignRepository scheduledCampaignRepository = mock(ScheduledCampaignRepository.class);

    private Clock clock;
    private String environment;
    private DataSet dataset;

    @BeforeEach
    public void setUp() {
        clock = Clock.systemDefaultZone();
        sut = new CampaignScheduler(campaignExecutionEngine, clock, scheduledCampaignRepository, Executors.newFixedThreadPool(2));
        environment = secure().nextAlphanumeric(10);
        dataset = DataSet.builder()
            .withId(secure().nextAlphanumeric(10))
            .withName(secure().nextAlphanumeric(10))
            .build();
    }

    @ParameterizedTest()
    @EnumSource(Frequency.class)
    void schedule_campaign_execution(Frequency frequency) {
        List<PeriodicScheduledCampaign> periodicScheduledCampaign = createPeriodicScheduledCampaigns(singletonList(frequency), environment, dataset.id);
        when(scheduledCampaignRepository.getAll())
            .thenReturn(
                periodicScheduledCampaign
            );

        sut.executeScheduledCampaigns();

        softAssertVerifies(of(
            // Execution called with scheduledCampaign parameter with user "auto"
            () -> verify(campaignExecutionEngine).executeScheduledCampaign(periodicScheduledCampaign.getFirst().campaignExecutionRequests.getFirst().campaignId(), environment, dataset.id, "auto"),
            // Last execution is removed
            () -> verify(scheduledCampaignRepository).removeById(periodicScheduledCampaign.getFirst().id),
            // Add next execution except for EMPTY frequency
            () -> {
                if (EMPTY.equals(frequency)) {
                    verify(scheduledCampaignRepository, times(0)).add(any());
                } else {
                    verify(scheduledCampaignRepository).add(periodicScheduledCampaign.getFirst().nextScheduledExecution());
                }
            }
        ));
    }

    @Test
    void should_not_throw_exception_when_runtime_exceptions_occur_retrieving_campaigns_to_execute() {
        when(scheduledCampaignRepository.getAll())
            .thenThrow(new RuntimeException("scheduledCampaignRepository.getAll()"));

        assertDoesNotThrow(
            () -> sut.executeScheduledCampaigns()
        );
        verify(scheduledCampaignRepository).getAll();
    }

    @Test
    void should_not_throw_exception_when_runtime_exceptions_occur_executing_campaigns() {
        List<PeriodicScheduledCampaign> periodicScheduledCampaigns = createPeriodicScheduledCampaigns(asList(Frequency.MONTHLY, Frequency.DAILY));
        when(scheduledCampaignRepository.getAll())
            .thenReturn(
                periodicScheduledCampaigns
            );
        when(campaignExecutionEngine.executeById(periodicScheduledCampaigns.getFirst().campaignExecutionRequests.getFirst().campaignId(), SCHEDULER_EXECUTE_USER))
            .thenThrow(new RuntimeException("campaignExecutionEngine.executeById"));

        assertDoesNotThrow(
            () -> sut.executeScheduledCampaigns()
        );

        verify(campaignExecutionEngine, times(periodicScheduledCampaigns.size())).executeScheduledCampaign(any(), any(), any(), any());
        verify(campaignExecutionEngine, times(periodicScheduledCampaigns.size())).executeScheduledCampaign(any(), any(), any(), any());
    }

    @Test
    void should_execute_sequentially_campaigns() {
        PeriodicScheduledCampaign sc1 = new PeriodicScheduledCampaign(1L, now(clock).minusSeconds(5), Frequency.HOURLY, environment, of(new CampaignExecutionRequest(11L, "campaign title 1", dataset.id), new CampaignExecutionRequest(22L, "campaign title 2", dataset.id)));

        List<PeriodicScheduledCampaign> periodicScheduledCampaigns = of(sc1);
        when(scheduledCampaignRepository.getAll())
            .thenReturn(
                periodicScheduledCampaigns
            );
        InOrder inOrder = inOrder(campaignExecutionEngine);

        sut.executeScheduledCampaigns();

        inOrder.verify(campaignExecutionEngine).executeScheduledCampaign(eq(periodicScheduledCampaigns.getFirst().campaignExecutionRequests.getFirst().campaignId()), eq(environment), eq(dataset.id), eq("auto"));
        inOrder.verify(campaignExecutionEngine).executeScheduledCampaign(eq(periodicScheduledCampaigns.getFirst().campaignExecutionRequests.get(1).campaignId()), eq(environment), eq(dataset.id), eq("auto"));
        verify(campaignExecutionEngine, times(2)).executeScheduledCampaign(any(), any(), any(), any());
    }

    private List<PeriodicScheduledCampaign> createPeriodicScheduledCampaigns(List<Frequency> frequencies) {
        return createPeriodicScheduledCampaigns(frequencies, null, null);
    }

    private List<PeriodicScheduledCampaign> createPeriodicScheduledCampaigns(List<Frequency> frequencies, String environment, String datasetId) {
        Random rand = new Random();
        return frequencies.stream()
            .map(f ->
                new PeriodicScheduledCampaign(rand.nextLong(), now(clock).minusSeconds(5), f, environment, of(new CampaignExecutionRequest(rand.nextLong(), "title", datasetId)))
            )
            .collect(toList());
    }
}
