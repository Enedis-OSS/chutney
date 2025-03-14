/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.engine.api.execution;

import com.chutneytesting.action.spi.injectable.ActionsConfiguration;
import com.chutneytesting.engine.domain.execution.ExecutionEngine;
import com.chutneytesting.engine.domain.execution.ExecutionManager;
import com.chutneytesting.engine.domain.execution.ScenarioExecution;
import com.chutneytesting.engine.domain.execution.StepDefinition;
import com.chutneytesting.engine.domain.execution.engine.Dataset;
import com.chutneytesting.engine.domain.execution.engine.Environment;
import com.chutneytesting.engine.domain.report.Reporter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmbeddedTestEngine implements TestEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedTestEngine.class);

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
            .subscribeOn(Schedulers.io())
            .map(StepExecutionReportMapper::toDto)
            .observeOn(Schedulers.io())
            .onErrorResumeNext(throwable -> {
                LOGGER.error("Error in receiveNotification for execution {}", executionId, throwable);
                return Observable.empty(); // Does not stop the flow in case of error
            });
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
