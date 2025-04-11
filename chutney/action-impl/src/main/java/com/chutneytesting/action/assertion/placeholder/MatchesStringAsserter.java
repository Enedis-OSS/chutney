/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import com.chutneytesting.action.spi.injectable.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchesStringAsserter extends GuardedPlaceholderAsserter {

    private static final String MATCHES_STRINGS = "$matches:";

    public MatchesStringAsserter(Guard... guards) {
        super(guards);
    }

    @Override
    public boolean canApply(String value) {
        return value.startsWith(MATCHES_STRINGS);
    }

    @Override
    public boolean assertGuardedValue(Logger logger, Object actual, Object expected) {
        String patternToFound = expected.toString().substring(MATCHES_STRINGS.length());
        logger.info("Verify " + actual + " matches " + patternToFound);

        Pattern pattern = Pattern.compile(patternToFound);
        Matcher matcher = pattern.matcher(actual.toString());
        return matcher.matches();
    }

}
