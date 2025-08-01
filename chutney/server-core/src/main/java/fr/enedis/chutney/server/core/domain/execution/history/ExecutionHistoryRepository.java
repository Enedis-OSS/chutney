/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution.history;

import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory.ExecutionSummary;
import fr.enedis.chutney.server.core.domain.execution.report.ReportNotFoundException;
import fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository storing execution executionHistory by scenario.
 **/
public interface ExecutionHistoryRepository {

    /**
     * Add a report for a given scenario.
     *
     * @return execution ID
     * @throws IllegalStateException when storage for scenario cannot be created
     **/
    ExecutionHistory.Execution store(String scenarioId, ExecutionHistory.DetachedExecution executionProperties) throws IllegalStateException;

    /**
     * @param scenarioIds
     * @return the last report. Key of the map are scenarioIds
     */
    Map<String, ExecutionSummary> getLastExecutions(List<String> scenarioIds);

    /**
     * @return last reports of the indicated scenario.
     **/
    List<ExecutionSummary> getExecutions(String scenarioId);

    List<ExecutionSummary> getExecutions();

    ExecutionSummary getExecutionSummary(Long executionId);

    /**
     * @return the matching {@link ExecutionHistory.Execution}
     */
    ExecutionHistory.Execution getExecution(String scenarioId, Long reportId) throws ReportNotFoundException;

    List<ExecutionHistory.ExecutionSummary> getExecutionReportMatchKeyword(String query);

    /**
     * Override a previously stored {@link ExecutionHistory.Execution}.
     */
    void update(String scenarioId, ExecutionHistory.Execution updatedExecution);

    int setAllRunningExecutionsToKO();

    List<ExecutionSummary> getExecutionsWithStatus(ServerReportStatus status);

    PurgeReport deleteExecutions(Set<Long> executionsIds);
}
