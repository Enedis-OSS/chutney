/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MathFunctionsTest {

    @Test
    void abs_short_should_raise_exception() {
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                short a = 2;
                MathFunctions.abs(a);
            }
        );
    }

    @ParameterizedTest
    @MethodSource("provideAbsArguments")
    void abs_minus_2_should_return_2(Number a, Number expected) {
        assertThat(MathFunctions.abs(a)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideMinArguments")
    void min_4_and_9_should_return_4(Number a, Number b, Number expected) {
        assertThat(MathFunctions.min(a, b)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideMaxArguments")
    void max_4_and_9_should_return_9(Number a, Number b, Number expected) {
        assertThat(MathFunctions.max(a, b)).isEqualTo(expected);
    }

    private static Stream<Arguments> provideAbsArguments() {
        return Stream.of(
            Arguments.of(-2d, 2d),
            Arguments.of(-2f, 2f),
            Arguments.of(-2, 2),
            Arguments.of(-2L, 2L)
        );
    }

    private static Stream<Arguments> provideMinArguments() {
        return Stream.of(
            Arguments.of(4d, 9d, 4d),
            Arguments.of(4f, 9f, 4f),
            Arguments.of(4, 9, 4),
            Arguments.of(4L, 9L, 4L)
        );
    }

    private static Stream<Arguments> provideMaxArguments() {
        return Stream.of(
            Arguments.of(4d, 9d, 9d),
            Arguments.of(4f, 9f, 9f),
            Arguments.of(4, 9, 9),
            Arguments.of(4L, 9L, 9L)
        );
    }
}
