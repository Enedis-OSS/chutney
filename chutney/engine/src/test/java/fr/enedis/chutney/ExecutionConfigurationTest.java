/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.ActionTemplateParserV2;
import fr.enedis.chutney.action.domain.ActionTemplateRegistry;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.engine.api.execution.DatasetDto;
import fr.enedis.chutney.engine.api.execution.EnvironmentDto;
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto.StepDefinitionRequestDto;
import fr.enedis.chutney.engine.api.execution.StatusDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.engine.api.execution.TestEngine;
import io.reactivex.rxjava3.core.Observable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ExecutionConfigurationTest {

    private final ExecutionConfiguration sut = new ExecutionConfiguration();
    private final String FAKE_ENV_NAME = "fakeEnv";
    private final EnvironmentDto FAKE_ENV = new EnvironmentDto(FAKE_ENV_NAME, emptyMap());
    private final DatasetDto dataset = new DatasetDto(Collections.emptyMap(), Collections.emptyList());

    @Test
    public void should_execute_scenario_async() {
        //G
        final TestEngine testEngine = sut.embeddedTestEngine();
        StepDefinitionRequestDto stepDefinition = createSucessStep();
        ExecutionRequestDto requestDto = new ExecutionRequestDto(stepDefinition, FAKE_ENV, dataset);

        //W
        Long executionId = testEngine.executeAsync(requestDto);
        Observable<StepExecutionReportDto> reports = testEngine.receiveNotification(executionId);
        List<StepExecutionReportDto> results = new ArrayList<>();
        reports.blockingSubscribe(results::add);

        //T
        StepExecutionReportDto lastReport = results.getLast();
        assertThat(lastReport).hasFieldOrPropertyWithValue("status", StatusDto.SUCCESS);
        assertThat(lastReport.environment).isEqualTo(FAKE_ENV.name());
    }

    @Test
    public void should_execute_scenario_sync() {
        //G
        final TestEngine testEngine = sut.embeddedTestEngine();
        StepDefinitionRequestDto stepDefinition = createSucessStep();
        ExecutionRequestDto requestDto = new ExecutionRequestDto(stepDefinition, FAKE_ENV, dataset);

        //W
        StepExecutionReportDto result = testEngine.execute(requestDto);

        //T
        assertThat(result).hasFieldOrPropertyWithValue("status", StatusDto.SUCCESS);
        assertThat(result.environment).isEqualTo(FAKE_ENV.name());
    }

    @Test
    public void should_pause_resume_stop_scenario() {
        //G
        final TestEngine testEngine = sut.embeddedTestEngine();
        StepDefinitionRequestDto stepDefinition = createScenarioForPause();
        ExecutionRequestDto requestDto = new ExecutionRequestDto(stepDefinition, FAKE_ENV, dataset);

        //W
        List<StepExecutionReportDto> results = new ArrayList<>();
        Long executionId = testEngine.executeAsync(requestDto);
        Observable<StepExecutionReportDto> reports = testEngine.receiveNotification(executionId);
        reports.blockingSubscribe(report -> {
            if (StatusDto.RUNNING.equals(report.steps.getFirst().status)) {
                testEngine.pauseExecution(executionId);
            } else if (StatusDto.PAUSED.equals(report.steps.get(1).status)) {
                testEngine.resumeExecution(executionId);
            } else if (StatusDto.RUNNING.equals(report.steps.get(1).status)) {
                testEngine.stopExecution(executionId);
            }
            results.add(report);
        });

        StepExecutionReportDto finalReport = results.getLast();
        // check scenario status
        assertThat(finalReport).hasFieldOrPropertyWithValue("status", StatusDto.STOPPED);
        // check first step status
        assertThat(finalReport.steps.getFirst()).hasFieldOrPropertyWithValue("status", StatusDto.SUCCESS);
        // check second step status
        assertThat(finalReport.steps.get(1)).hasFieldOrPropertyWithValue("status", StatusDto.STOPPED);
        // check third step status
        assertThat(finalReport.steps.get(2)).hasFieldOrPropertyWithValue("status", StatusDto.STOPPED);
        assertThat(finalReport.environment).isEqualTo(FAKE_ENV.name());
    }

    @Test
    public void should_stop_immediately_a_running_step() {
        StepExecutionReportDto finalReport = executeAndStopWhen(
            scenario(createSleepsStep("long sleep", "30 s"), createSleepsStep("never executed", "1 ms")),
            report -> StatusDto.RUNNING.equals(report.steps.getFirst().status),
            (testEngine, executionId) -> testEngine.stopExecution(executionId)
        );

        assertThat(finalReport.status).isEqualTo(StatusDto.STOPPED);
        assertThat(finalReport.steps).extracting(step -> step.status).containsExactly(StatusDto.STOPPED, StatusDto.STOPPED);
        assertThat(finalReport.steps.getFirst().information).contains("Stop requested while executing this step");
    }

    @Test
    public void should_stop_immediately_a_step_waiting_for_its_retry() {
        StepDefinitionRequestDto failingStepWithLongRetryDelay = new StepDefinitionRequestDto(
            "retried step",
            null,
            new ExecutionRequestDto.StepStrategyDefinitionRequestDto("retry-with-timeout", Map.of("timeOut", "60 s", "retryDelay", "30 s")),
            "fail",
            Collections.emptyMap(),
            null,
            Collections.emptyMap(),
            Collections.emptyMap()
        );

        StepExecutionReportDto finalReport = executeAndStopWhen(
            scenario(failingStepWithLongRetryDelay),
            report -> StatusDto.FAILURE.equals(report.steps.getFirst().status),
            // Reports are notified by the engine thread, delay to stop while it sleeps before retrying
            (testEngine, executionId) -> CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> testEngine.stopExecution(executionId))
        );

        assertThat(finalReport.status).isEqualTo(StatusDto.STOPPED);
    }

    @Test
    public void should_stop_immediately_a_paused_scenario() {
        StepExecutionReportDto finalReport = executeAndStopWhen(
            scenario(createSleepsStep("sleep", "100 ms"), createSleepsStep("paused", "1 ms")),
            report -> report.steps.stream().anyMatch(step -> StatusDto.PAUSED.equals(step.status)),
            (testEngine, executionId) -> testEngine.stopExecution(executionId),
            (testEngine, executionId) -> testEngine.pauseExecution(executionId)
        );

        assertThat(finalReport.status).isEqualTo(StatusDto.STOPPED);
    }

    private StepExecutionReportDto executeAndStopWhen(StepDefinitionRequestDto scenario,
                                                      Predicate<StepExecutionReportDto> stopCondition,
                                                      BiConsumer<TestEngine, Long> stop) {
        return executeAndStopWhen(scenario, stopCondition, stop, (testEngine, executionId) -> {});
    }

    private StepExecutionReportDto executeAndStopWhen(StepDefinitionRequestDto scenario,
                                                      Predicate<StepExecutionReportDto> stopCondition,
                                                      BiConsumer<TestEngine, Long> stop,
                                                      BiConsumer<TestEngine, Long> onStart) {
        final TestEngine testEngine = sut.embeddedTestEngine();
        Long executionId = testEngine.executeAsync(new ExecutionRequestDto(scenario, FAKE_ENV, dataset));
        onStart.accept(testEngine, executionId);

        AtomicBoolean stopRequested = new AtomicBoolean();
        return testEngine.receiveNotification(executionId)
            .doOnNext(report -> {
                if (stopCondition.test(report) && stopRequested.compareAndSet(false, true)) {
                    stop.accept(testEngine, executionId);
                }
            })
            .timeout(10, TimeUnit.SECONDS)
            .blockingLast();
    }

    private StepDefinitionRequestDto scenario(StepDefinitionRequestDto... steps) {
        return new StepDefinitionRequestDto(
            "scenario name",
            null,
            null,
            null,
            Collections.emptyMap(),
            List.of(steps),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    @Test
    public void should_catch_exception_in_fault_barrier_engine() {
        //G
        final TestEngine testEngine = sut.embeddedTestEngine();
        final ActionTemplateRegistry actionTemplateRegistry = sut.actionTemplateRegistry();
        ActionTemplate actionTemplate = new ActionTemplateParserV2().parse(ErrorAction.class).result();
        Map<String, ActionTemplate> actionTemplatesByType = (Map<String, ActionTemplate>) ReflectionTestUtils.getField(actionTemplateRegistry, "actionTemplatesByType");
        actionTemplatesByType.put("error", actionTemplate);

        StepDefinitionRequestDto stepDefinition = new StepDefinitionRequestDto(
            "throw runtime exception step",
            null,
            null,
            "error",
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        ExecutionRequestDto requestDto = new ExecutionRequestDto(stepDefinition, FAKE_ENV, dataset);

        //W
        StepExecutionReportDto result = testEngine.execute(requestDto);

        //T
        assertThat(result).hasFieldOrPropertyWithValue("status", StatusDto.FAILURE);
        assertThat(result.errors.getFirst()).isEqualTo("Action [error] failed: Should be catch by fault barrier");
    }

    @Test
    public void should_shutdown_threads_on_close() throws Exception {
        //G
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        ExecutionConfiguration executionConfiguration = new ExecutionConfiguration(5L, executorService, emptyMap(), null, null);

        //W
        executionConfiguration.embeddedTestEngine().close();

        //T
        assertThat(executorService.isShutdown()).isTrue();
    }

    private StepDefinitionRequestDto createSucessStep() {
        return new StepDefinitionRequestDto(
            "scenario name",
            null,
            null,
            "success",
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    private StepDefinitionRequestDto createScenarioForPause() {
        List<StepDefinitionRequestDto> steps = new ArrayList<>();
        steps.add(createSleepsStep("sleep 1"));
        steps.add(createSleepsStep("sleep 2"));
        steps.add(createSleepsStep("sleep 3"));
        return new StepDefinitionRequestDto(
            "scenario name",
            null,
            null,
            null,
            Collections.emptyMap(),
            steps,
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    private StepDefinitionRequestDto createSleepsStep(String name) {
        return createSleepsStep(name, "1 s");
    }

    private StepDefinitionRequestDto createSleepsStep(String name, String duration) {
        return new StepDefinitionRequestDto(
            name,
            null,
            null,
            "sleep",
            Maps.newHashMap("duration", duration),
            null,
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    public static class ErrorAction implements Action {

        public ErrorAction() {
        }

        @Override
        public ActionExecutionResult execute() {
            throw new RuntimeException("Should be catch by fault barrier");
        }
    }
}
