/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution;

import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistory;
import fr.enedis.chutney.server.core.domain.execution.report.ScenarioExecutionReport;
import fr.enedis.chutney.server.core.domain.execution.report.StepExecutionReportCore;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotParsableException;


public class ScenarioExecutionEngine {

    private final ServerTestEngine executionEngine;
    private final ScenarioExecutionEngineAsync executionEngineAsync;


    public ScenarioExecutionEngine(ServerTestEngine executionEngine,
                                   ScenarioExecutionEngineAsync executionEngineAsync) {
        this.executionEngineAsync = executionEngineAsync;
        this.executionEngine = executionEngine;
    }

    /**
     * Retrieves a scenario from it's ID, executes it on  ExecutionEngine and store StepExecutionReport.
     *
     * @param executionRequest The request execution.
     * @return an execution Report.
     */
    public ScenarioExecutionReport execute(ExecutionRequest executionRequest) throws ScenarioNotFoundException, ScenarioNotParsableException {
        return executionEngineAsync.followExecution(executionRequest.testCase.id(), executionEngineAsync.execute(executionRequest)).blockingLast();
    }

    public ScenarioExecutionReport simpleSyncExecution(ExecutionRequest executionRequest) {

        StepExecutionReportCore finalStepReport = executionEngine.execute(executionRequest);
        return new ScenarioExecutionReport(0L, executionRequest.testCase.metadata().title(), executionRequest.environment, executionRequest.userId, executionRequest.tags, executionRequest.dataset, finalStepReport);
    }

    public ExecutionHistory.Execution saveNotExecutedScenarioExecution(ExecutionRequest executionRequest) {
        return executionEngineAsync.saveNotExecutedScenarioExecution(executionRequest);
    }

}
