/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import com.chutneytesting.action.spi.injectable.Logger;

public class ContainsAsserter extends GuardedPlaceholderAsserter {

    private static final String CONTAINS = "$contains:";

    public ContainsAsserter(Guard... guards) {
        super(guards);
    }

    @Override
    public boolean canApply(String value) {
        return value.startsWith(CONTAINS);
    }

    @Override
    public boolean assertGuardedValue(Logger logger, Object actual, Object expected) {
        String toFind = expected.toString().substring(CONTAINS.length());
        logger.info("Verify " + actual + " contains " + toFind);
        return actual.toString().contains(toFind);
    }

}
