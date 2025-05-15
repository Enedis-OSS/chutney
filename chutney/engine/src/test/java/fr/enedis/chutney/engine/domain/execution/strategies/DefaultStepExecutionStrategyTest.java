/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.strategies;

import static fr.enedis.chutney.engine.api.execution.StatusDto.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.ExecutionConfiguration;
import fr.enedis.chutney.action.domain.ActionTemplateLoaders;
import fr.enedis.chutney.action.domain.DefaultActionTemplateRegistry;
import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.engine.api.execution.StepExecutionReportDto;
import fr.enedis.chutney.engine.api.execution.TestEngine;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.StepDefinition;
import fr.enedis.chutney.engine.domain.execution.TestActionTemplateLoader;
import fr.enedis.chutney.engine.domain.execution.engine.DefaultStepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.StepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.evaluation.StepDataEvaluator;
import fr.enedis.chutney.engine.domain.execution.engine.scenario.ScenarioContextImpl;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.evaluation.SpelFunctions;
import fr.enedis.chutney.engine.domain.execution.report.Status;
import fr.enedis.chutney.tools.Jsons;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DefaultStepExecutionStrategyTest {

    private DefaultStepExecutionStrategy strategy = DefaultStepExecutionStrategy.instance;
    private StepDataEvaluator dataEvaluator = new StepDataEvaluator(new SpelFunctions());
    private StepExecutor stepExecutor = new DefaultStepExecutor(new DefaultActionTemplateRegistry(new ActionTemplateLoaders(Collections.singletonList(new TestActionTemplateLoader()))));

    @Test
    public void should_execute_the_step() {
        Step step = mock(Step.class);
        when(step.subSteps()).thenReturn(Lists.newArrayList());

        strategy.execute(null, step, null, null);

        verify(step, times(1)).execute(any(), any(), any());
    }

    @Test
    public void when_all_execution_succeed_global_status_is_ok() {
        Step step = buildSample("success", "success");

        Status status = strategy.execute(ScenarioExecution.createScenarioExecution(null), step, new ScenarioContextImpl(), new StepExecutionStrategies(Sets.newHashSet(DefaultStepExecutionStrategy.instance)));
        Assertions.assertThat(status).isEqualTo(Status.SUCCESS);

        Map<String, Status> expectedStatusByStepName = new HashMap<>();
        expectedStatusByStepName.put("sample 1", Status.SUCCESS);
        expectedStatusByStepName.put("step 1", Status.SUCCESS);
        expectedStatusByStepName.put("step 1.1", Status.SUCCESS);
        expectedStatusByStepName.put("step 1.1.1", Status.SUCCESS);
        expectedStatusByStepName.put("step 2", Status.SUCCESS);
        expectedStatusByStepName.put("step 2.1", Status.SUCCESS);

        SoftAssertions softly = new SoftAssertions();
        visit(step, subStep -> softly.assertThat(subStep.status()).isEqualTo(expectedStatusByStepName.get(getStepName(subStep))));
        softly.assertAll();
    }

    @Test
    public void when_early_step_execution_fails_global_status_is_ko_and_next_steps_are_not_runned() {
        Step step = buildSample("fail", "fail");

        Status status = strategy.execute(ScenarioExecution.createScenarioExecution(null), step, new ScenarioContextImpl(), new StepExecutionStrategies(Sets.newHashSet(DefaultStepExecutionStrategy.instance)));
        Assertions.assertThat(status).isEqualTo(Status.FAILURE);

        Map<String, Status> expectedStatusByStepName = new HashMap<>();
        expectedStatusByStepName.put("sample 1", Status.FAILURE);
        expectedStatusByStepName.put("step 1", Status.FAILURE);
        expectedStatusByStepName.put("step 1.1", Status.FAILURE);
        expectedStatusByStepName.put("step 1.1.1", Status.FAILURE);
        expectedStatusByStepName.put("step 2", Status.NOT_EXECUTED);
        expectedStatusByStepName.put("step 2.1", Status.NOT_EXECUTED);

        SoftAssertions softly = new SoftAssertions();
        visit(step, subStep -> softly.assertThat(subStep.status()).isEqualTo(expectedStatusByStepName.get(getStepName(subStep))));
        softly.assertAll();
    }

    @Test
    public void when_later_step_execution_fails_global_status_is_ko_meanwhile_first_steps_are_ok() {
        Step step = buildSample("success", "fail");

        Status status = strategy.execute(ScenarioExecution.createScenarioExecution(null), step, new ScenarioContextImpl(), new StepExecutionStrategies(Sets.newHashSet(DefaultStepExecutionStrategy.instance)));
        Assertions.assertThat(status).isEqualTo(Status.FAILURE);

        Map<String, Status> expectedStatusByStepName = new HashMap<>();
        expectedStatusByStepName.put("sample 1", Status.FAILURE);
        expectedStatusByStepName.put("step 1", Status.SUCCESS);
        expectedStatusByStepName.put("step 1.1", Status.SUCCESS);
        expectedStatusByStepName.put("step 1.1.1", Status.SUCCESS);
        expectedStatusByStepName.put("step 2", Status.FAILURE);
        expectedStatusByStepName.put("step 2.1", Status.FAILURE);

        SoftAssertions softly = new SoftAssertions();
        visit(step, subStep -> softly.assertThat(subStep.status()).isEqualTo(expectedStatusByStepName.get(getStepName(subStep))));
        softly.assertAll();
    }

    @Test
    public void should_resolve_name_from_context_with_default_strategy() {
        // G
        final TestEngine testEngine = new ExecutionConfiguration().embeddedTestEngine();
        ExecutionRequestDto requestDto = Jsons.loadJsonFromClasspath("scenarios_examples/defaultStrategy/default_strategy_step_with_name_resolver_from_context_put.json", ExecutionRequestDto.class);

        // W
        StepExecutionReportDto result = testEngine.execute(requestDto);

        // T
        assertThat(result).hasFieldOrPropertyWithValue("status", SUCCESS);
        assertThat(result.steps).hasSize(2);
        assertThat(result.steps.get(1).name).isEqualTo("Step 2 Parent : value");
    }

    private static String getStepName(Step step) {
        StepDefinition stepDefinition = (StepDefinition) ReflectionTestUtils.getField(step, "definition");
        return (String) ReflectionTestUtils.getField(stepDefinition, "name");
    }

    private Step buildSample(String type_111, String type_21) {
        return buildStep("sample 1", "fake-type",
            buildStep("step 1", "fake-type", buildStep("step 1.1", "fake-type", buildStep("step 1.1.1", type_111))),
            buildStep("step 2", "fake-type", buildStep("step 2.1", type_21))
        );
    }

    private Step buildStep(String name, String type, Step... subSteps) {
        return new Step(dataEvaluator, buildStepDef(name, type), stepExecutor, Arrays.asList(subSteps));
    }

    private StepDefinition buildStepDef(String name, String type) {
        return new StepDefinition(name, null, type, null, null, null, null, null);
    }

    private static void visit(Step step, Consumer<Step> action) {
        step.subSteps().forEach(subStep -> visit(subStep, action));
        action.accept(step);
    }
}
