/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.placeholder;

import fr.enedis.chutney.action.spi.injectable.Logger;

public abstract class GuardedPlaceholderAsserter implements PlaceholderAsserter {

    private final Guard[] guards;

    public GuardedPlaceholderAsserter(Guard... guards) {
        this.guards = guards;
    }

    @Override
    public boolean assertValue(Logger logger, Object actual, Object expected) {
        for (Guard guard : guards) {
            if (!guard.guardAssertValue(logger, actual)) {
                return false;
            }
        }
        return assertGuardedValue(logger, actual, expected);
    }

    protected abstract boolean assertGuardedValue(Logger logger, Object actual, Object expected);

    public interface Guard {
        /**
         * Guard actual value for placeholder assertion.<br/>
         * Returns true if guard is ok.<br/>
         * When false, use logger to trace the guard assert.
         */
        boolean guardAssertValue(Logger logger, Object actual);

        Guard ACTUAL_NOT_NULL_GUARD = (logger, actual) -> {
            if (actual == null) {
                logger.error("Actual value is null");
                return false;
            }
            return true;
        };
    }
}
