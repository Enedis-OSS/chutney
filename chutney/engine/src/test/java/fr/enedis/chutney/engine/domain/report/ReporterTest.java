/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.report;

import static fr.enedis.chutney.engine.domain.execution.report.Status.PAUSED;
import static fr.enedis.chutney.engine.domain.execution.report.Status.RUNNING;
import static fr.enedis.chutney.engine.domain.execution.report.Status.SUCCESS;
import static fr.enedis.chutney.tools.WaitUtils.awaitDuring;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.RxBus;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.evaluation.SpelFunctions;
import fr.enedis.chutney.engine.domain.execution.event.EndScenarioExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.event.StartScenarioExecutionEvent;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReporterTest {

    private final Target fakeTarget = TargetImpl.NONE;
    private final StepDataEvaluator dataEvaluator = new StepDataEvaluator(new SpelFunctions());

    private Reporter sut;
    private Step step;
    private ScenarioExecution scenarioExecution;

    @BeforeEach
    public void before() {
        step = buildFakeScenario();
        sut = new Reporter();
        scenarioExecution = ScenarioExecution.createScenarioExecution(null);
    }

    @Test
    void testConcurrentModificationOnSubSteps() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Runnable getter = () -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        step.removeStepExecution();
                        step.addStepExecution(buildFakeScenario());
                        MILLISECONDS.sleep(5);
                    }
                } catch (Exception e) {
                   // should not happen
                }
            };

            executor.submit(getter);
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 500) {
                StepExecutionReport report = sut.generateReport(step, Step::status, "env");
                if (Status.FAILURE.equals(report.status)) {
                    fail();
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void parent_status_should_be_recalculate() {
        Step subStep1 = step.subSteps().getFirst();
        Step subSubStep1 = step.subSteps().getFirst().subSteps().getFirst();
        Step subSubStep2 = step.subSteps().getFirst().subSteps().get(1);

        StepExecutionReport report = sut.generateReport(step, Step::status, "env");
        assertThat(report.status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.getFirst().status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.getFirst().steps.getFirst().status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.getFirst().steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);

        step.beginExecution(scenarioExecution);
        subStep1.beginExecution(scenarioExecution);
        subSubStep1.beginExecution(scenarioExecution);
        report = sut.generateReport(step, Step::status, "env");
        assertThat(report.status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().steps.getFirst().status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);

        subSubStep1.pauseExecution(scenarioExecution);
        report = sut.generateReport(step, Step::status, "env");
        assertThat(report.status).isEqualTo(Status.PAUSED);
        assertThat(report.steps.getFirst().status).isEqualTo(Status.PAUSED);
        assertThat(report.steps.getFirst().steps.getFirst().status).isEqualTo(Status.PAUSED);
        assertThat(report.steps.getFirst().steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);

        subSubStep1.success();
        report = sut.generateReport(step, Step::status, "env");
        assertThat(report.status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().steps.getFirst().status).isEqualTo(SUCCESS);
        assertThat(report.steps.getFirst().steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);
        assertThat(report.steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);

        subSubStep2.beginExecution(scenarioExecution);
        report = sut.generateReport(step, Step::status, "env");
        assertThat(report.status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().steps.getFirst().status).isEqualTo(SUCCESS);
        assertThat(report.steps.getFirst().steps.get(1).status).isEqualTo(RUNNING);
        assertThat(report.steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);

        subSubStep2.success();
        subStep1.endExecution(scenarioExecution);
        report = sut.generateReport(step, Step::status, "env");
        assertThat(report.status).isEqualTo(RUNNING);
        assertThat(report.steps.getFirst().status).isEqualTo(SUCCESS);
        assertThat(report.steps.getFirst().steps.getFirst().status).isEqualTo(SUCCESS);
        assertThat(report.steps.getFirst().steps.get(1).status).isEqualTo(SUCCESS);
        assertThat(report.steps.get(1).status).isEqualTo(Status.NOT_EXECUTED);
    }

    @Test
    public void should_publish_report_when_scenario_execution_notifications() {
        sut.createPublisher(scenarioExecution.executionId, mock(Step.class));
        TestObserver<StepExecutionReport> scenarioExecutionReportObservable = sut.subscribeOnExecution(scenarioExecution.executionId).test();
        scenarioExecutionReportObservable.assertValueCount(0);

        executeFakeScenarioSuccess();
        scenarioExecutionReportObservable.awaitCount(10);

        scenarioExecutionReportObservable.dispose();
    }

    @Test
    public void should_retain_report_for_late_subscription() {
        sut.setRetentionDelaySeconds(1);
        sut.createPublisher(scenarioExecution.executionId, mock(Step.class));
        executeFakeScenarioSuccess();

        awaitDuring(500, MILLISECONDS);

        TestObserver<StepExecutionReport> scenarioExecutionReportObservable =
            sut.subscribeOnExecution(scenarioExecution.executionId).test();

        scenarioExecutionReportObservable.assertComplete();
        scenarioExecutionReportObservable.assertValueCount(1);

        scenarioExecutionReportObservable.dispose();
    }

    @Test
    public void should_return_empty_observable_for_unknown_execution_id() {
        TestObserver<StepExecutionReport> scenarioExecutionReportObservable =
            sut.subscribeOnExecution(0L).test();

        scenarioExecutionReportObservable.assertValueCount(0);

        scenarioExecutionReportObservable.dispose();
    }

    @Test
    public void should_calculate_root_step_only_when_scenario_end_else_running() {
        Step subStep1 = step.subSteps().getFirst();
        Step subStep11 = step.subSteps().getFirst().subSteps().getFirst();
        Step subStep12 = step.subSteps().getFirst().subSteps().get(1);
        Step subStep2 = step.subSteps().get(1);

        sut.createPublisher(scenarioExecution.executionId, step);
        TestObserver<Status> observer = sut.subscribeOnExecution(scenarioExecution.executionId).map(report -> report.status).test();
        RxBus.getInstance().post(new StartScenarioExecutionEvent(scenarioExecution, step));//1

        step.beginExecution(scenarioExecution);//2
        subStep1.beginExecution(scenarioExecution);//3
        subStep11.beginExecution(scenarioExecution);//4
        subStep11.success();
        subStep11.endExecution(scenarioExecution);//5
        subStep12.beginExecution(scenarioExecution);//6
        subStep12.success();
        subStep12.endExecution(scenarioExecution);//7
        subStep1.endExecution(scenarioExecution);//8
        subStep2.beginExecution(scenarioExecution);//9
        subStep2.pauseExecution(scenarioExecution);//10
        subStep2.success();//simulate a resume
        subStep2.endExecution(scenarioExecution);//11
        step.endExecution(scenarioExecution);//12

        RxBus.getInstance().post(new EndScenarioExecutionEvent(scenarioExecution, step));//13
        await().atMost(20, SECONDS).untilAsserted(() ->
            observer.assertResult(RUNNING, RUNNING, RUNNING, RUNNING, RUNNING, RUNNING, RUNNING, RUNNING, RUNNING, PAUSED, RUNNING, RUNNING, SUCCESS)
        );
        assertThat(step.status()).isEqualTo(SUCCESS);
    }

    private Step buildFakeScenario() {
        List<StepDefinition> subSubSteps = new ArrayList<>();
        StepDefinition subSubStepDef1 = new StepDefinition("fakeStep1", fakeTarget, "actionType", null, null, null, null, null);
        StepDefinition subSubStepDef2 = new StepDefinition("fakeStep2", fakeTarget, "actionType", null, null, null, null, null);
        subSubSteps.add(subSubStepDef1);
        subSubSteps.add(subSubStepDef2);
        StepDefinition subStepDef1 = new StepDefinition("fakeParentStep", fakeTarget, "actionType", null, null, subSubSteps, null, null);
        StepDefinition subStepDef2 = new StepDefinition("fakeParentEmptyStep", fakeTarget, "actionType", null, null, null, null, null);
        List<StepDefinition> steps = new ArrayList<>();
        steps.add(subStepDef1);
        steps.add(subStepDef2);
        StepDefinition rootStepDefinition = new StepDefinition("fakeScenario", fakeTarget, "actionType", null, null, steps, null, null);

        return buildStep(rootStepDefinition);
    }

    private Step buildStep(StepDefinition definition) {
        final List<Step> steps = definition.steps.stream().map(this::buildStep).collect(Collectors.toList());
        return new Step(dataEvaluator, definition, null, steps);
    }

    private void executeFakeScenarioSuccess() {
        Step subStep1 = step.subSteps().getFirst();
        Step subSubStep1 = step.subSteps().getFirst().subSteps().getFirst();
        Step subSubStep2 = step.subSteps().getFirst().subSteps().get(1);

        RxBus.getInstance().post(new StartScenarioExecutionEvent(scenarioExecution, step));
        step.beginExecution(scenarioExecution);
        subStep1.beginExecution(scenarioExecution);
        subSubStep1.beginExecution(scenarioExecution);
        subSubStep1.success();
        subSubStep1.endExecution(scenarioExecution);
        subSubStep2.beginExecution(scenarioExecution);
        subSubStep2.success();
        subSubStep2.endExecution(scenarioExecution);
        subStep1.endExecution(scenarioExecution);
        step.endExecution(scenarioExecution);
        RxBus.getInstance().post(new EndScenarioExecutionEvent(scenarioExecution, step));
    }
}
