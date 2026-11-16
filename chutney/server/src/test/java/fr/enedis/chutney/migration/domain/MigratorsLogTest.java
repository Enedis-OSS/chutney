/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.migration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.enedis.chutney.campaign.infra.CampaignJpaRepository;
import fr.enedis.chutney.campaign.infra.index.CampaignIndexRepository;
import fr.enedis.chutney.dataset.infra.FileDatasetRepository;
import fr.enedis.chutney.dataset.infra.index.DatasetIndexRepository;
import fr.enedis.chutney.execution.infra.storage.ScenarioExecutionReportJpaRepository;
import fr.enedis.chutney.execution.infra.storage.index.ExecutionReportIndexRepository;
import fr.enedis.chutney.migration.infra.ExecutionReportRepository;
import fr.enedis.chutney.scenario.infra.index.ScenarioIndexRepository;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.scenario.infra.raw.ScenarioJpaRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

public class MigratorsLogTest {

    private final Logger migrationLogger = (Logger) LoggerFactory.getLogger("fr.enedis.chutney.migration");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    public void captureLogs() {
        logs.start();
        migrationLogger.addAppender(logs);
    }

    @AfterEach
    public void releaseLogs() {
        migrationLogger.detachAppender(logs);
    }

    @Test
    public void should_not_log_when_there_is_no_scenario_to_index() {
        ScenarioJpaRepository scenarioJpaRepository = mock(ScenarioJpaRepository.class);
        when(scenarioJpaRepository.findByActivated(eq(true), any(Pageable.class))).thenReturn(new SliceImpl<>(List.of()));

        new ScenarioMigrator(scenarioJpaRepository, mock(ScenarioIndexRepository.class)).migrate();

        assertThat(logs.list).isEmpty();
    }

    @Test
    public void should_not_log_when_there_is_no_campaign_to_index() {
        CampaignJpaRepository campaignJpaRepository = mock(CampaignJpaRepository.class);
        when(campaignJpaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        new CampaignMigrator(campaignJpaRepository, mock(CampaignIndexRepository.class)).migrate();

        assertThat(logs.list).isEmpty();
    }

    @Test
    public void should_not_log_when_there_is_no_dataset_to_index() {
        new DatasetMigrator(mock(FileDatasetRepository.class), mock(DatasetIndexRepository.class)).migrate();

        assertThat(logs.list).isEmpty();
    }

    @Test
    public void should_not_log_when_there_is_no_report_to_index() {
        ScenarioExecutionReportJpaRepository reportJpaRepository = mock(ScenarioExecutionReportJpaRepository.class);
        when(reportJpaRepository.findByScenarioExecutionScenarioIdIn(anyList(), any(Pageable.class))).thenReturn(new SliceImpl<>(List.of()));

        new ExecutionReportMigrator(
            mock(ExecutionReportRepository.class),
            reportJpaRepository,
            mock(ExecutionReportIndexRepository.class),
            mock(ScenarioJpaRepository.class)
        ).migrate();

        assertThat(logs.list).isEmpty();
    }

    @Test
    public void should_index_every_page_and_log_when_there_are_scenarios() {
        ScenarioJpaRepository scenarioJpaRepository = mock(ScenarioJpaRepository.class);
        ScenarioIndexRepository scenarioIndexRepository = mock(ScenarioIndexRepository.class);
        PageRequest firstPage = PageRequest.of(0, 10);
        List<ScenarioEntity> firstScenarios = List.of(mock(ScenarioEntity.class));
        List<ScenarioEntity> lastScenarios = List.of(mock(ScenarioEntity.class));
        when(scenarioJpaRepository.findByActivated(true, firstPage)).thenReturn(new SliceImpl<>(firstScenarios, firstPage, true));
        when(scenarioJpaRepository.findByActivated(true, firstPage.next())).thenReturn(new SliceImpl<>(lastScenarios, firstPage.next(), false));

        new ScenarioMigrator(scenarioJpaRepository, scenarioIndexRepository).migrate();

        verify(scenarioIndexRepository).saveAll(firstScenarios);
        verify(scenarioIndexRepository).saveAll(lastScenarios);
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("Start indexing scenario...", "2 scenario successfully indexed");
    }
}
