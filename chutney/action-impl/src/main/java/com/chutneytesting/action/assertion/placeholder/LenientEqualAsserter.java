/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import static com.chutneytesting.action.common.JsonUtils.lenientEqual;

import com.chutneytesting.action.spi.injectable.Logger;
import com.jayway.jsonpath.JsonPath;

public class LenientEqualAsserter extends GuardedPlaceholderAsserter {

    private static final String IS_LENIENT_EQUAL = "$lenientEqual:";

    public LenientEqualAsserter(Guard... guards) {
        super(guards);
    }

    @Override
    public boolean canApply(String value) {
        return value.startsWith(IS_LENIENT_EQUAL);
    }

    @Override
    public boolean assertGuardedValue(Logger logger, Object actual, Object expected) {
        String expect = expected.toString().substring(IS_LENIENT_EQUAL.length());
        Object expectedRead = JsonPath.parse(expect).read("$");
        return lenientEqual(actual, expectedRead, true);
    }
}
