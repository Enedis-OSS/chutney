/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.instrument.infra;

import static io.micrometer.core.instrument.Tag.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.campaign.Campaign;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import fr.enedis.chutney.server.core.domain.scenario.campaign.ScenarioExecutionCampaign;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
class MicrometerMetrics implements ChutneyMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Map<ServerReportStatus, AtomicLong>> statusCountCache = new HashMap<>();

    MicrometerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onScenarioExecutionEnded(TestCase testCase, ExecutionHistory.Execution execution) {
        final String scenarioId = testCase.metadata().id();
        final List<String> tags = testCase.metadata().tags();
        final ServerReportStatus status = execution.status();
        final long duration = execution.duration();

        final String tagsAsString = StringUtils.join(tags, "|");
        final Counter scenarioExecutionCount = this.meterRegistry.counter("scenario_execution_count", asList(of("scenarioId", scenarioId), of("status", status.name()), of("tags", tagsAsString)));
        scenarioExecutionCount.increment();

        final Timer scenarioExecutionTimer = this.meterRegistry.timer("scenario_execution_timer", asList(of("scenarioId", scenarioId), of("status", status.name()), of("tags", tagsAsString)));
        scenarioExecutionTimer.record(duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onCampaignExecutionEnded(Campaign campaign, CampaignExecution campaignExecution) {
        final String campaignId = campaign.id.toString();
        final Map<ServerReportStatus, Long> campaignCountByStatus = campaignExecution.scenarioExecutionReports().stream().collect(groupingBy(ScenarioExecutionCampaign::status, counting()));
        final ServerReportStatus status = campaignExecution.status();
        final long campaignDuration = campaignExecution.getDuration();

        final Counter campaignExecutionCount = this.meterRegistry.counter("campaign_execution_count", asList(of("campaignId", campaignId), of("campaignTitle", campaign.title), of("status", status.name())));
        campaignExecutionCount.increment();

        final Timer campaignExecutionTimer = this.meterRegistry.timer("campaign_execution_timer", singleton(of("campaignId", campaignId)));
        campaignExecutionTimer.record(campaignDuration, TimeUnit.MILLISECONDS);

        final Map<ServerReportStatus, AtomicLong> cachedMetrics = getMetricsInCache(campaignId);
        updateMetrics(campaignCountByStatus, cachedMetrics);
    }

    @Override
    public void onHttpError(HttpStatusCode status) {
        final Counter httpErrorCount = this.meterRegistry.counter("http_error", List.of(of("status", String.valueOf(status.value()))));
        httpErrorCount.increment();
    }

    private void updateMetrics(Map<ServerReportStatus, Long> scenarioCountByStatus, Map<ServerReportStatus, AtomicLong> cachedMetrics) {
        cachedMetrics.forEach((key, value) -> {
            final Long valueInCache = scenarioCountByStatus.get(key);
            value.set(Objects.requireNonNullElse(valueInCache, 0L));
        });
    }

    private Map<ServerReportStatus, AtomicLong> getMetricsInCache(String campaignId) {
        Map<ServerReportStatus, AtomicLong> cachedMetrics = statusCountCache.get(campaignId);
        if (cachedMetrics == null) {
            cachedMetrics = new HashMap<>();

            final Map<ServerReportStatus, AtomicLong> tmp = cachedMetrics;
            Arrays.asList(ServerReportStatus.values()).forEach(s -> {
                final AtomicLong initialValue = new AtomicLong(0);
                this.meterRegistry.gauge("scenario_in_campaign_gauge", asList(of("campaignId", campaignId), of("scenarioStatus", s.name())), initialValue);
                tmp.put(s, initialValue);
            });

            cachedMetrics.putAll(tmp);
            statusCountCache.put(campaignId, cachedMetrics);
        }
        return cachedMetrics;
    }
}
