/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.api.execution;

import fr.enedis.chutney.action.spi.injectable.ActionsConfiguration;
import fr.enedis.chutney.engine.domain.execution.ExecutionEngine;
import fr.enedis.chutney.engine.domain.execution.ExecutionManager;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import fr.enedis.chutney.engine.domain.execution.engine.Dataset;
import fr.enedis.chutney.engine.domain.execution.engine.Environment;
import fr.enedis.chutney.engine.domain.report.Reporter;
import io.reactivex.rxjava3.core.Observable;
import java.util.Optional;

public final class EmbeddedTestEngine implements TestEngine {

    private final ExecutionEngine engine;
    private final Reporter reporter;
    private final ExecutionManager executionManager;
    private final ActionsConfiguration actionsConfiguration;

    public EmbeddedTestEngine(ExecutionEngine engine, Reporter reporter, ExecutionManager executionManager, ActionsConfiguration actionsConfiguration) {
        this.engine = engine;
        this.reporter = reporter;
        this.executionManager = executionManager;
        this.actionsConfiguration = actionsConfiguration;
    }

    @Override
    public StepExecutionReportDto execute(ExecutionRequestDto request) {
        Long executionId = executeAsync(request);
        return receiveNotification(executionId).blockingLast();
    }

    @Override
    public Long executeAsync(ExecutionRequestDto request) {
        StepDefinition stepDefinition = StepDefinitionMapper.toStepDefinition(request.scenario.definition);
        Dataset dataset = Optional.ofNullable(request.dataset)
            .map(d -> new Dataset(d.constants, d.datatable))
            .orElseGet(Dataset::new);
        Environment environment = EnvironmentDtoMapper.INSTANCE.toDomain(request.environment);
        return engine.execute(
            stepDefinition,
            dataset,
            ScenarioExecution.createScenarioExecution(actionsConfiguration),
            environment);
    }

    @Override
    public Observable<StepExecutionReportDto> receiveNotification(Long executionId) {
        return reporter.subscribeOnExecution(executionId)
            .map(StepExecutionReportMapper::toDto);
    }

    @Override
    public void pauseExecution(Long executionId) {
        executionManager.pauseExecution(executionId);
    }

    @Override
    public void resumeExecution(Long executionId) {
        executionManager.resumeExecution(executionId);
    }

    @Override
    public void stopExecution(Long executionId) {
        executionManager.stopExecution(executionId);
    }

    @Override
    public void close() {
        engine.shutdown();
    }
}
