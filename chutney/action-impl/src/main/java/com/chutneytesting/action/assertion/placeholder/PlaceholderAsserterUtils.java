/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion.placeholder;

import static com.chutneytesting.action.assertion.placeholder.GuardedPlaceholderAsserter.Guard.ActualNotNullGuard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaceholderAsserterUtils {

    private static final List<PlaceholderAsserter> asserters = new ArrayList<>();

    static {
        asserters.add(new IsNullAsserter());
        asserters.add(new NotNullAsserter());
        asserters.add(new ContainsAsserter(ActualNotNullGuard));
        asserters.add(new BeforeDateAsserter(ActualNotNullGuard));
        asserters.add(new AfterDateAsserter(ActualNotNullGuard));
        asserters.add(new EqualDateAsserter(ActualNotNullGuard));
        asserters.add(new MatchesStringAsserter(ActualNotNullGuard));
        asserters.add(new LessThanAsserter(ActualNotNullGuard));
        asserters.add(new GreaterThanAsserter(ActualNotNullGuard));
        asserters.add(new ValueArrayAsserter());
        asserters.add(new IsEmptyAsserter(ActualNotNullGuard));
        asserters.add(new LenientEqualAsserter(ActualNotNullGuard));
    }

    public static Optional<PlaceholderAsserter> getAsserterMatching(Object toMatch) {
        if (toMatch == null) {
            return Optional.of(new IsNullAsserter());
        }
        return asserters.stream().filter(a -> a.canApply(toMatch.toString())).findFirst();
    }
}
