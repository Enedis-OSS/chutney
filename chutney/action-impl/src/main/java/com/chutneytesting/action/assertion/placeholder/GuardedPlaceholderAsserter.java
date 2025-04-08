/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import com.chutneytesting.action.spi.injectable.Logger;
import java.util.Arrays;

public abstract class GuardedPlaceholderAsserter implements PlaceholderAsserter {

    private final Guard[] guards;

    public GuardedPlaceholderAsserter(Guard... guards) {
        this.guards = guards;
    }

    @Override
    public boolean assertValue(Logger logger, Object actual, Object expected) {
        boolean guardResult = Arrays.stream(guards)
            .map(g -> g.guardAssertValue(logger, actual, expected))
            .anyMatch(b -> !b);
        if (guardResult) return false;
        return assertGuardedValue(logger, actual, expected);
    }

    protected abstract boolean assertGuardedValue(Logger logger, Object actual, Object expected);

    public interface Guard {
        boolean guardAssertValue(Logger logger, Object actual, Object expected);

        Guard ActualNotNullGuard = (logger, actual, expected) -> actual != null;
    }
}
