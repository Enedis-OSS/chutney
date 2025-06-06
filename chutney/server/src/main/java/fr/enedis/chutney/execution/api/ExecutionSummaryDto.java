/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.Attached;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionProperties;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.WithScenario;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as= ImmutableExecutionSummaryDto.class)
@JsonDeserialize(as= ImmutableExecutionSummaryDto.class)
public interface ExecutionSummaryDto extends ExecutionProperties, Attached, WithScenario {

    static List<ExecutionSummaryDto> toDto(Collection<ExecutionSummary> executionSummaryList) {
        return executionSummaryList.stream().map(ExecutionSummaryDto::toDto).collect(Collectors.toList());
    }

    static ExecutionSummaryDto toDto(ExecutionSummary executionSummary) {
        return ImmutableExecutionSummaryDto.builder()
            .time(executionSummary.time())
            .duration(executionSummary.duration())
            .status(executionSummary.status())
            .info(executionSummary.info())
            .error(executionSummary.error())
            .executionId(executionSummary.executionId())
            .testCaseTitle(executionSummary.testCaseTitle())
            .environment(executionSummary.environment())
            .dataset(executionSummary.dataset())
            .user(executionSummary.user())
            .campaignReport(executionSummary.campaignReport())
            .scenarioId(executionSummary.scenarioId())
            .tags(executionSummary.tags())
            .build();
    }

    static List<ExecutionSummary> fromDto(Collection<ExecutionSummaryDto> executionSummaryList) {
        return executionSummaryList.stream().map(ExecutionSummaryDto::fromDto).collect(Collectors.toList());
    }

    static ExecutionSummary fromDto(ExecutionSummaryDto dto) {
        return ImmutableExecutionHistory.ExecutionSummary.builder()
            .time(dto.time())
            .duration(dto.duration())
            .status(dto.status())
            .info(dto.info())
            .error(dto.error())
            .executionId(dto.executionId())
            .testCaseTitle(dto.testCaseTitle())
            .environment(dto.environment())
            .user(dto.user())
            .scenarioId(dto.scenarioId())
            .tags(dto.tags())
            .build();
    }
}
