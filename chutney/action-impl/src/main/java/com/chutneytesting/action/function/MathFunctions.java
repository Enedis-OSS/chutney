/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.function;

import com.chutneytesting.action.spi.SpelFunction;

public class MathFunctions {

    @SpelFunction
    public static Number abs(Number a) {
        String numberType = a.getClass().getSimpleName();

        return switch (numberType) {
            case "Double" -> Math.abs(a.doubleValue());
            case "Float" -> Math.abs(a.floatValue());
            case "Integer" -> Math.abs(a.intValue());
            case "Long" -> Math.abs(a.longValue());
            default -> throw new IllegalArgumentException("Unexpected type: " + numberType);
        };
    }

    @SpelFunction
    public static Number min(Number a, Number b) {
        String numberType = a.getClass().getSimpleName();

        return switch (numberType) {
            case "Double" -> Math.min(a.doubleValue(), b.doubleValue());
            case "Float" -> Math.min(a.floatValue(), b.floatValue());
            case "Integer" -> Math.min(a.intValue(), b.intValue());
            case "Long" -> Math.min(a.longValue(), b.longValue());
            default -> throw new IllegalArgumentException("Unexpected type: " + numberType);
        };
    }

    @SpelFunction
    public static Number max(Number a, Number b) {
        String numberType = a.getClass().getSimpleName();

        return switch (numberType) {
            case "Double" -> Math.max(a.doubleValue(), b.doubleValue());
            case "Float" -> Math.max(a.floatValue(), b.floatValue());
            case "Integer" -> Math.max(a.intValue(), b.intValue());
            case "Long" -> Math.max(a.longValue(), b.longValue());
            default -> throw new IllegalArgumentException("Unexpected type: " + numberType);
        };
    }
}
