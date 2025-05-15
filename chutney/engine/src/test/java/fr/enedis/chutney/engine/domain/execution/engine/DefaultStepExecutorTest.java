/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.ok;
import static fr.enedis.chutney.engine.domain.execution.ScenarioExecution.createScenarioExecution;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.action.TestActionTemplateFactory.ComplexAction;
import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.ActionTemplateParserV2;
import fr.enedis.chutney.action.domain.ActionTemplateRegistry;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DefaultStepExecutorTest {

    @Test
    public void should_execute_the_fake_action() {
        ActionTemplateRegistry actionTemplateRegistry = mock(ActionTemplateRegistry.class);
        ActionTemplate actionTemplate = mock(ActionTemplate.class, RETURNS_DEEP_STUBS);
        when(actionTemplate.create(any()).validateInputs()).thenReturn(emptyList());
        when(actionTemplate.create(any()).execute()).thenReturn(ok());
        when(actionTemplateRegistry.getByIdentifier(any())).thenReturn(of(actionTemplate));
        Step step = mock(Step.class, RETURNS_DEEP_STUBS);

        StepExecutor stepExecutor = new DefaultStepExecutor(actionTemplateRegistry);
        stepExecutor.execute(createScenarioExecution(null), mock(TargetImpl.class), step);

        verify(actionTemplate.create(any()), times(1)).execute();
        verify(step, times(0)).failure(any(Exception.class));
    }

    @Test
    public void should_fail_step_with_message_on_action_error() {
        ActionTemplateRegistry actionTemplateRegistry = mock(ActionTemplateRegistry.class);
        ActionTemplate actionTemplate = mock(ActionTemplate.class, RETURNS_DEEP_STUBS);
        when(actionTemplate.create(any()).execute()).thenThrow(RuntimeException.class);
        when(actionTemplate.create(any()).validateInputs()).thenReturn(emptyList());
        when(actionTemplateRegistry.getByIdentifier(any())).thenReturn(of(actionTemplate));
        Step step = mock(Step.class, RETURNS_DEEP_STUBS);

        StepExecutor stepExecutor = new DefaultStepExecutor(actionTemplateRegistry);
        stepExecutor.execute(createScenarioExecution(null), mock(TargetImpl.class), step);

        verify(step, times(1)).failure("Action [null] failed: java.lang.RuntimeException");
    }

    @Test
    public void should_fail_step_with_message_on_validation_error() {
        ActionTemplateRegistry actionTemplateRegistry = mock(ActionTemplateRegistry.class);
        ActionTemplate actionTemplate = mock(ActionTemplate.class, RETURNS_DEEP_STUBS);
        when(actionTemplate.create(any()).validateInputs()).thenReturn(singletonList("validation error"));
        when(actionTemplateRegistry.getByIdentifier(any())).thenReturn(of(actionTemplate));
        Step step = mock(Step.class, RETURNS_DEEP_STUBS);

        StepExecutor stepExecutor = new DefaultStepExecutor(actionTemplateRegistry);
        stepExecutor.execute(createScenarioExecution(null), mock(TargetImpl.class), step);

        verify(step, times(1)).failure("validation error");
    }

    @Test
    public void should_execute_a_real_action() {
        ActionTemplateRegistry actionTemplateRegistry = mock(ActionTemplateRegistry.class);
        ActionTemplate actionTemplate = new ActionTemplateParserV2().parse(ComplexAction.class).result();

        when(actionTemplateRegistry.getByIdentifier(any())).thenReturn(of(actionTemplate));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("stringParam", "teststring");
        inputs.put("param1", "a");
        inputs.put("param2", "b");

        Step step = mock(Step.class, RETURNS_DEEP_STUBS);
        when(step.getEvaluatedInputs()).thenReturn(inputs);

        StepExecutor stepExecutor = new DefaultStepExecutor(actionTemplateRegistry);
        stepExecutor.execute(createScenarioExecution(null), mock(TargetImpl.class), step);

        verify(step, times(0)).failure(any(Exception.class));
    }
}
