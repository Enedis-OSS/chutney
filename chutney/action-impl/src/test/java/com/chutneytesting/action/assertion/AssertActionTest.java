/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion;

import static com.chutneytesting.action.spi.ActionExecutionResult.Status.Failure;
import static com.chutneytesting.action.spi.ActionExecutionResult.Status.Success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chutneytesting.action.TestLogger;
import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Logger;
import com.chutneytesting.action.spi.injectable.StepDefinitionSpi;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AssertActionTest {

    public static Object[] parametersForShould_success_when_true_and_failed_when_false() {
        return new Object[]{
            new Object[]{"assertion def that returns true", true, Success},
            new Object[]{"assertion def that returns false", false, Failure}
        };
    }

    @ParameterizedTest
    @MethodSource("parametersForShould_success_when_true_and_failed_when_false")
    public void should_success_when_true_and_failed_when_false(String assertionDef, Boolean value, ActionExecutionResult.Status expected) {
        // Given
        Map<String, Boolean> evaluatedAssertion = Map.of("assert-true", value);
        List<Map<String, Boolean>> evaluatedAssertions = List.of(evaluatedAssertion);

        Map<String, String> assertion = Map.of("assert-true", assertionDef);
        List<Map<String, String>> assertions = List.of(assertion);

        StepDefinitionSpi stepDefinition = mock(StepDefinitionSpi.class);
        when(stepDefinition.inputs()).thenReturn(Map.of("asserts", assertions));

        Logger logger = mock(Logger.class);

        // When
        AssertAction assertAction = new AssertAction(logger, evaluatedAssertions, stepDefinition);
        ActionExecutionResult result = assertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(expected);
        String logMessage = assertionDef + " is " + StringUtils.capitalize(value.toString());
        if (value) {
            verify(logger).info(logMessage);
        } else {
            verify(logger).error(logMessage);
        }
    }

    @Test
    void soft_assertions() {
        // Given
        TestLogger logger = new TestLogger();
        List<Map<String, Boolean>> evaluatedAssertions = List.of(
            Map.of("assert-true", false),
            Map.of("assert-true", false)
        );
        List<Map<String, String>> assertionsDef = List.of(
            Map.of("assert-true", "some expression"),
            Map.of("assert-true", "some expression")
        );
        StepDefinitionSpi stepDefinition = mock(StepDefinitionSpi.class);
        when(stepDefinition.inputs()).thenReturn(Map.of("asserts", assertionsDef));

        // When
        AssertAction assertAction = new AssertAction(logger, evaluatedAssertions, stepDefinition);
        ActionExecutionResult result = assertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
        assertThat(logger.info).isEmpty();
        assertThat(logger.errors).hasSize(2);
    }
}
