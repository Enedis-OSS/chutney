/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.step;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.RxBus;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import fr.enedis.chutney.engine.domain.execution.engine.StepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.EvaluationException;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContext;
import fr.enedis.chutney.engine.domain.execution.event.BeginStepExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.EndStepExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.PauseStepExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import fr.enedis.chutney.engine.domain.execution.strategies.StepStrategyDefinition;
import fr.enedis.chutney.tools.Try;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes {@link #definition} and {@link #state}  of a Step.
 */
public class Step {

    private static final Logger LOGGER = LoggerFactory.getLogger(Step.class);

    private final StepDefinition definition;

    private final StepState state;
    private final List<Step> steps;
    private Target target;
    private final StepExecutor executor;
    private final StepDataEvaluator dataEvaluator;
    private StepContext stepContext;

    public Step(StepDataEvaluator dataEvaluator, StepDefinition definition, StepExecutor executor, List<Step> steps) {
        this.dataEvaluator = dataEvaluator;
        this.definition = definition;
        this.target = definition.getTarget().orElse(TargetImpl.NONE);
        this.executor = executor;
        this.steps = steps;
        this.state = new StepState(definition.name);
        this.stepContext = new StepContext();
    }

    public static Step nonExecutable(StepDefinition definition) {
        return new Step(null, definition, null, emptyList()); // TODO any - Type a NonExecutableStep, or a RootStep at least
    }

    public Status execute(ScenarioExecution scenarioExecution, ScenarioContext scenarioContext) {
        return this.execute(scenarioExecution, scenarioContext, emptyMap());
    }

    public Status execute(ScenarioExecution scenarioExecution, ScenarioContext scenarioContext, Map<String, Object> localContext) {

        if (scenarioExecution.hasToPause()) {
            final Instant startPauseInstant = Instant.now();
            pauseExecution(scenarioExecution);
            scenarioExecution.waitForRestart();
            state.resumeExecution();
            state.addInformation("Step pause from " + startPauseInstant + " to " + Instant.now());
        }

        if (scenarioExecution.hasToStop()) {
            stopExecution(scenarioExecution);
            return Status.STOPPED;
        }

        beginExecution(scenarioExecution);

        try {
            Map<String, Object> evaluationContext = buildEvaluationContext(scenarioContext, localContext);
            final Map<String, Object> evaluatedInputs = definition.type.equals("final") ? definition.inputs() : unmodifiableMap(dataEvaluator.evaluateNamedDataWithContextVariables(definition.inputs(), evaluationContext));
            target = dataEvaluator.evaluateTarget(target, evaluationContext);
            resolveName(evaluationContext);
            Try
                .exec(() -> this.stepContext = new StepContext(scenarioContext, localContext, evaluatedInputs))
                .ifSuccess(stepContextExecuted -> {
                    executor.execute(scenarioExecution, target, this);
                    if (Status.SUCCESS.equals(this.state.status())) {
                        executeStepValidations(stepContextExecuted);
                    }
                    this.stepContext = stepContextExecuted.copy();
                })
                .ifFailed(this::failure);
        } catch (EvaluationException e) {
            failure(e);
        } catch (RuntimeException e) {
            failure(e);
            LOGGER.warn("Intercepted exception!", e);
        } finally {
            endExecution(scenarioExecution);
        }
        return state.status();
    }

    public void beginExecution(ScenarioExecution scenarioExecution) {
        state.beginExecution();
        RxBus.getInstance().post(new BeginStepExecutionEvent(scenarioExecution, this));
    }

    public void endExecution(ScenarioExecution scenarioExecution) {
        state.endExecution(isParentStep());
        RxBus.getInstance().post(new EndStepExecutionEvent(scenarioExecution, this));
    }

    public void stopExecution(ScenarioExecution scenarioExecution) {
        state.addInformation("Stop requested before executing this step");
        state.stopExecution();
        RxBus.getInstance().post(new EndStepExecutionEvent(scenarioExecution, this));
    }

    public void pauseExecution(ScenarioExecution scenarioExecution) {
        state.pauseExecution();
        RxBus.getInstance().post(new PauseStepExecutionEvent(scenarioExecution, this));
    }

    public String name() {
        return this.state.name();
    }

    public void resolveName(Map<String, Object> context) {
        this.state.setName(dataEvaluator.silentEvaluateString(state.name(), context));
    }

    public Status status() {
        if (isParentStep()) {
            final Status worstSubStepsStatus = Status.worst(subStepsStatus());
            if (Status.PAUSED.equals(worstSubStepsStatus)) {
                return Status.PAUSED;
            }
            if (Status.RUNNING.equals(state.status())) {
                return Status.RUNNING;
            }
            return worstSubStepsStatus;
        }
        return state.status();
    }

    private List<Status> subStepsStatus() {
        if (!isParentStep() || Status.FAILURE.equals(state.status())) {
            return Lists.newArrayList(state.status());
        } else {
            return new ArrayList<>(this.steps).stream()
                .filter(Objects::nonNull)
                .map(Step::status)
                .collect(Collectors.toList());
        }
    }

    public void addInformation(String... info) {
        state.addInformation(info);
    }

    public void addErrorMessage(String... errors) {
        state.addErrors(errors);
    }

    public void failure(Throwable e) {
        failure(ofNullable(e.getMessage()).orElse(e.toString()));
    }

    public void failure(String... message) {
        state.errorOccurred(message);
    }

    public void success(String... message) {
        state.successOccurred(message);
    }

    public void resetExecution() {
        state.reset();
        steps.forEach(Step::resetExecution);
    }

    public void startWatch() {
        state.startWatch();
    }

    public void stopWatch() {
        state.stopWatch();
    }

    public Duration duration() {
        return state.duration();
    }

    public Instant startDate() {
        return state.startDate();
    }

    public List<String> informations() {
        return state.informations();
    }

    public List<String> errors() {
        return state.errors();
    }

    public Target target() {
        return target;
    }

    public StepDefinition definition() {
        return definition;
    }

    public Optional<StepStrategyDefinition> strategy() {
        return definition.getStrategy();
    }

    public String type() {
        return definition.type;
    }

    public List<Step> subSteps() {
        return Collections.unmodifiableList(steps);
    }

    public StepExecutor executor() {
        return executor;
    }

    public StepDataEvaluator dataEvaluator() {
        return dataEvaluator;
    }

    public Boolean isForStrategyApplied() {
        return this.state.isForStrategyApplied();
    }

    public void setIsForStrategyApplied(Boolean isForStrategyApplied) {
        this.state.setIsForStrategyApplied(isForStrategyApplied);
    }

    public boolean isParentStep() {
        return !steps.isEmpty();
    }

    public void updateContextFrom(StepExecutionReport remoteReport) {
        ActionExecutionResult.Status status = Status.SUCCESS.equals(remoteReport.status) ? ActionExecutionResult.Status.Success : ActionExecutionResult.Status.Failure;
        updateContextWith(status, remoteReport.stepResults, emptyList(), emptyList());
    }

    public void updateContextFrom(ActionExecutionResult actionResult) {
        updateContextWith(actionResult.status, actionResult.outputs, emptyList(), emptyList());
    }

    private Map<String, Object> buildEvaluationContext(ScenarioContext scenarioContext, Map<String, Object> localContext) {
        Map<String, Object> evaluationContext = new HashMap<>();
        evaluationContext.putAll(scenarioContext);
        evaluationContext.putAll(localContext);
        evaluationContext.put("target", target);
        return evaluationContext;
    }

    private void updateContextWith(ActionExecutionResult.Status status, Map<String, Object> actionOutputs, List<String> information, List<String> errors) {
        if (status == ActionExecutionResult.Status.Success) {
            Try.exec(() -> {
                    updateContextWithActionOutputs(actionOutputs);
                    updateContextWithDefinitionOutputs();
                    this.success();
                    return null;
                })
                .ifFailed(e -> failure("Cannot evaluate outputs."
                    + " - Exception: " + e.getClass() + " with message: \"" + e.getMessage() + "\"")
                );
        } else {
            this.failure(Lists.newArrayList(errors).toArray(new String[errors.size()]));
        }

        this.addInformation(Lists.newArrayList(information).toArray(new String[information.size()]));
    }

    private void updateContextWithActionOutputs(Map<String, Object> actionOutputs) {
        this.stepContext.addStepOutputs(actionOutputs);
        this.stepContext.addScenarioContext(actionOutputs);
    }

    private void updateContextWithDefinitionOutputs() {
        final Map<String, Object> evaluatedOutputs = dataEvaluator.evaluateNamedDataWithContextVariables(definition.outputs, stepContext.evaluationContext());
        this.stepContext.addStepOutputs(evaluatedOutputs);
        this.stepContext.addScenarioContext(evaluatedOutputs);
    }

    private void executeStepValidations(StepContext stepContext) {
        Try.exec(() -> {
                final Map<String, Object> evaluatedValidations = dataEvaluator.evaluateNamedDataWithContextVariables(definition.validations, stepContext.evaluationContext());
                evaluatedValidations.forEach((k, v) -> {
                    if (!(boolean) v) {
                        failure("Validation [" + k + " : " + definition.validations.get(k).toString() + "] : KO");
                    } else {
                        state.addInformation("Validation [" + k + " : " + definition.validations.get(k).toString() + "] : OK");
                    }
                });
                return null;
            })
            .ifFailed(e -> failure("Step validation failed."
                + " - Exception: " + e.getClass() + " with message: \"" + e.getMessage() + "\""));
    }

    public void addStepExecution(Step step) {
        this.steps.add(step);
    }

    public Map<String, Object> getEvaluatedInputs() {
        return unmodifiableMap(this.stepContext.getEvaluatedInputs());
    }

    public Map<String, Object> getScenarioContext() {
        return this.stepContext.getScenarioContext().unmodifiable();
    }

    public Map<String, Object> getStepOutputs() {
        return unmodifiableMap(this.stepContext.getStepOutputs());
    }

    public Map<String, Object> getStepContextInputSnapshot() {
        return this.stepContext.getStepContextSnapshot().getInputsSnapshot();
    }

    public Map<String, Object> getStepContextOutputSnapshot() {
        return this.stepContext.getStepContextSnapshot().getOutputsSnapshot();
    }

    public void removeStepExecution() {
        this.steps.clear();
    }

}
