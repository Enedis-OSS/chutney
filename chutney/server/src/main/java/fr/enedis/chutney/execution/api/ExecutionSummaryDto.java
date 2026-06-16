/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.enedis.chutney.server.core.domain.dataset.DataSet;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import fr.enedis.chutney.server.core.domain.scenario.campaign.CampaignExecution;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableExecutionSummaryDto.class)
@JsonDeserialize(as = ImmutableExecutionSummaryDto.class)
public interface ExecutionSummaryDto {

    @JsonProperty("executionId")
    Long executionId();

    @JsonProperty("scenarioId")
    String scenarioId();

    @JsonProperty("time")
    LocalDateTime time();

    @JsonProperty("duration")
    long duration();

    @JsonProperty("status")
    ServerReportStatus status();

    @JsonProperty("info")
    Optional<String> info();

    @JsonProperty("error")
    Optional<String> error();

    @JsonProperty("testCaseTitle")
    String testCaseTitle();

    @JsonProperty("environment")
    String environment();

    @JsonProperty("dataset")
    Optional<DataSet> dataset();

    @JsonProperty("user")
    String user();

    @JsonProperty("campaignReport")
    Optional<CampaignExecution> campaignReport();

    @JsonProperty("tags")
    Optional<Set<String>> tags();

    static List<ExecutionSummaryDto> toDto(Collection<ExecutionSummary> executionSummaryList) {
        return executionSummaryList.stream().map(ExecutionSummaryDto::toDto).collect(Collectors.toList());
    }

    static ExecutionSummaryDto toDto(ExecutionSummary executionSummary) {
        return ImmutableExecutionSummaryDto.builder()
            .executionId(executionSummary.executionId())
            .scenarioId(executionSummary.scenarioId())
            .time(executionSummary.time())
            .duration(executionSummary.duration())
            .status(executionSummary.status())
            .info(executionSummary.info())
            .error(executionSummary.error())
            .testCaseTitle(executionSummary.testCaseTitle())
            .environment(executionSummary.environment())
            .dataset(executionSummary.dataset())
            .user(executionSummary.user())
            .campaignReport(executionSummary.campaignReport())
            .tags(executionSummary.tags())
            .build();
    }

    static List<ExecutionSummary> fromDto(Collection<ExecutionSummaryDto> executionSummaryList) {
        return executionSummaryList.stream().map(ExecutionSummaryDto::fromDto).collect(Collectors.toList());
    }

    static ExecutionSummary fromDto(ExecutionSummaryDto dto) {
        return ImmutableExecutionHistory.ExecutionSummary.builder()
            .executionId(dto.executionId())
            .scenarioId(dto.scenarioId())
            .time(dto.time())
            .duration(dto.duration())
            .status(dto.status())
            .info(dto.info())
            .error(dto.error())
            .testCaseTitle(dto.testCaseTitle())
            .environment(dto.environment())
            .user(dto.user())
            .campaignReport(dto.campaignReport())
            .tags(dto.tags())
            .build();
    }
}
