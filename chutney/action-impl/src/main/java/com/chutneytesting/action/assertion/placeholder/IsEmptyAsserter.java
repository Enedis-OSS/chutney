/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import com.chutneytesting.action.spi.injectable.Logger;
import net.minidev.json.JSONArray;

public class IsEmptyAsserter extends GuardedPlaceholderAsserter {

    private static final String IS_EMPTY = "$isEmpty";

    public IsEmptyAsserter(Guard... guards) {
        super(guards);
    }

    @Override
    public boolean canApply(String value) {
        return IS_EMPTY.equals(value);
    }

    @Override
    public boolean assertGuardedValue(Logger logger, Object actual, Object expected) {
        return assertValue(logger, actual);
    }

    public boolean assertValue(Logger logger, Object actual) {
        logger.info("Verify " + actual + " is empty");
        if (actual instanceof JSONArray jsonArray) {
            return jsonArray.stream().map(e -> assertValue(logger, e)).reduce((a, b) -> a && b).orElse(true);
        } else {
            return actual.toString().isEmpty();
        }
    }
}
