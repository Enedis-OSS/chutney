/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import com.chutneytesting.action.spi.injectable.Logger;
import java.text.NumberFormat;
import java.text.ParseException;

public class LessThanAsserter extends GuardedPlaceholderAsserter {

    private static final String IS_LESS_THAN = "$isLessThan:";
    private static final NumberFormat nb = NumberFormat.getInstance();

    public LessThanAsserter(Guard... guards) {
        super(guards);
    }

    @Override
    public boolean canApply(String value) {
        return value.startsWith(IS_LESS_THAN);
    }

    @Override
    public boolean assertGuardedValue(Logger logger, Object actual, Object expected) {
        String expect = expected.toString().substring(IS_LESS_THAN.length());
        try {
            Number numActual = nb.parse(actual.toString().replaceAll(" ", ""));
            Number numExpected = nb.parse(expect.replaceAll(" ", ""));
            logger.info("Verify " + numActual.doubleValue() + " < " + numExpected.doubleValue());
            return numActual.doubleValue() < numExpected.doubleValue();
        } catch (ParseException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

}
