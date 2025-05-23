/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.strategies;

import static fr.enedis.chutney.engine.api.execution.StatusDto.SUCCESS;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.ExecutionConfiguration;
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.engine.api.execution.TestEngine;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContextImpl;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.tools.Jsons;
import org.junit.jupiter.api.Test;

public class SoftAssertStrategyTest {

    private final StepExecutionStrategy sut = new SoftAssertStrategy();

    @Test
    public void should_run_all_substeps_regardless_of_failure_when_using_soft_assert_on_parent_step() {
        // Given
        Step failureStep = mock(Step.class);
        when(failureStep.execute(any(), any(), any())).thenReturn(Status.FAILURE);
        Step failureStep2 = mock(Step.class);
        when(failureStep2.execute(any(), any(), any())).thenThrow(new RuntimeException());
        Step successStep = mock(Step.class);
        when(successStep.execute(any(), any(), any())).thenReturn(Status.SUCCESS);

        Step rootStep = mock(Step.class);
        when(rootStep.subSteps()).thenReturn(newArrayList(failureStep, failureStep2, successStep));
        when(rootStep.isParentStep()).thenReturn(true);
        when(rootStep.dataEvaluator()).thenReturn(new StepDataEvaluator(null));

        StepExecutionStrategies strategies = mock(StepExecutionStrategies.class);
        when(strategies.buildStrategyFrom(any())).thenReturn(DefaultStepExecutionStrategy.instance);

        // When
        Status actualStatus = sut.execute(null, rootStep, new ScenarioContextImpl(), strategies);

        // Then
        verify(failureStep).execute(any(), any(), any());
        verify(failureStep2).execute(any(), any(), any());
        verify(successStep).execute(any(), any(), any());
        assertThat(actualStatus).isEqualTo(Status.WARN);
    }

    @Test
    public void should_run_next_step_when_current_step_fails_with_soft_assert() {
        // Given
        Step failureStepWithSoftAssert = mock(Step.class);
        when(failureStepWithSoftAssert.execute(any(), any())).thenReturn(Status.FAILURE);

        Step nextStep = mock(Step.class);
        when(nextStep.execute(any(), any())).thenReturn(Status.SUCCESS);

        Step rootStep = mock(Step.class);
        when(rootStep.subSteps()).thenReturn(newArrayList(failureStepWithSoftAssert, nextStep));
        when(rootStep.isParentStep()).thenReturn(true);
        when(rootStep.dataEvaluator()).thenReturn(new StepDataEvaluator(null));

        StepExecutionStrategies strategies = mock(StepExecutionStrategies.class);
        when(strategies.buildStrategyFrom(rootStep)).thenReturn(DefaultStepExecutionStrategy.instance);
        when(strategies.buildStrategyFrom(failureStepWithSoftAssert)).thenReturn(sut);
        when(strategies.buildStrategyFrom(nextStep)).thenReturn(DefaultStepExecutionStrategy.instance);

        // When
        DefaultStepExecutionStrategy.instance.execute(null, rootStep, new ScenarioContextImpl(), strategies);

        // Then
        verify(failureStepWithSoftAssert).execute(any(), any(), any());
        verify(nextStep).execute(any(), any(), any());
    }

    @Test
    public void should_resolve_name_from_context_with_strategy_soft_assert() {
        // G
        final TestEngine testEngine = new ExecutionConfiguration().embeddedTestEngine();
        ExecutionRequestDto requestDto = Jsons.loadJsonFromClasspath("scenarios_examples/softAssertStrategy/soft_assert_strategy_step_with_name_resolver_from_context_put.json", ExecutionRequestDto.class);

        // W
        StepExecutionReportDto result = testEngine.execute(requestDto);

        // T
        assertThat(result).hasFieldOrPropertyWithValue("status", SUCCESS);
        assertThat(result.steps).hasSize(2);
        assertThat(result.steps.get(1).name).isEqualTo("Step 2 Parent : value");
    }

}
