/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.server.core.domain.execution.processor;

import com.chutneytesting.server.core.domain.execution.ExecutionRequest;
import com.chutneytesting.server.core.domain.scenario.TestCase;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;

public interface TestCasePreProcessor<T extends TestCase> {

    T apply(ExecutionRequest executionRequest);

    default boolean test(T testCase) {
        Type type = ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        return ((Class<?>) type).isAssignableFrom(testCase.getClass());
    }
}
