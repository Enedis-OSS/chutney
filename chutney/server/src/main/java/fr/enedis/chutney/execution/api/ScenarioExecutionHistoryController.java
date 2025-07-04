/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import fr.enedis.chutney.server.core.domain.execution.RunningScenarioExecutionDeleteException;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import fr.enedis.chutney.server.core.domain.execution.history.ImmutableExecutionHistory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ScenarioExecutionHistoryController {

    private final ExecutionHistoryRepository executionHistoryRepository;

    ScenarioExecutionHistoryController(ExecutionHistoryRepository executionHistoryRepository) {
        this.executionHistoryRepository = executionHistoryRepository;
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/api/ui/scenario/{scenarioId}/execution/v1", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ExecutionSummaryDto> listExecutions(@PathVariable("scenarioId") String scenarioId) {
        return ExecutionSummaryDto.toDto(
            executionHistoryRepository.getExecutions(scenarioId));
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/api/ui/scenario/execution/{executionId}/summary/v1", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExecutionSummaryDto getExecutionSummary(@PathVariable("executionId") Long executionId) {
        return ExecutionSummaryDto.toDto(executionHistoryRepository.getExecutionSummary(executionId));
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/api/ui/scenario/{scenarioId}/execution/{executionId}/v1", produces = MediaType.APPLICATION_JSON_VALUE)
    public ExecutionHistory.Execution getExecutionReport(@PathVariable("scenarioId") String scenarioId, @PathVariable("executionId") Long executionId) {
        ExecutionHistory.Execution execution = executionHistoryRepository.getExecution(scenarioId, executionId); // TODO - return ExecutionReportDto
        if (execution.dataset().isPresent()
            && execution.dataset().get().id == null
            && (execution.dataset().get().datatable == null || execution.dataset().get().datatable.isEmpty())
            && (execution.dataset().get().constants == null || execution.dataset().get().constants.isEmpty())) {
            return ImmutableExecutionHistory.Execution.copyOf(execution).withDataset(Optional.empty());
        }
        return execution;
    }

    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE')")
    @DeleteMapping(path = "/api/ui/scenario/execution/{executionId}")
    public void deleteExecution(@PathVariable("executionId") Long executionId) {
        var report = executionHistoryRepository.deleteExecutions(Set.of(executionId));
        if (report.scenariosExecutionsIds().isEmpty()) {
            throw new RunningScenarioExecutionDeleteException(executionId);
        }
    }
}
