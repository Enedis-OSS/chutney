/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.placeholder;

import static fr.enedis.chutney.action.assertion.placeholder.GuardedPlaceholderAsserter.Guard.ACTUAL_NOT_NULL_GUARD;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaceholderAsserterUtils {

    private static final List<PlaceholderAsserter> asserters = new ArrayList<>();

    static {
        asserters.add(new IsNullAsserter());
        asserters.add(new NotNullAsserter());
        asserters.add(new ContainsAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new BeforeDateAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new AfterDateAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new EqualDateAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new MatchesStringAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new LessThanAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new GreaterThanAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new ValueArrayAsserter());
        asserters.add(new IsEmptyAsserter(ACTUAL_NOT_NULL_GUARD));
        asserters.add(new LenientEqualAsserter(ACTUAL_NOT_NULL_GUARD));
    }

    public static Optional<PlaceholderAsserter> getAsserterMatching(Object toMatch) {
        if (toMatch == null) {
            return Optional.of(new IsNullAsserter());
        }
        return asserters.stream().filter(a -> a.canApply(toMatch.toString())).findFirst();
    }
}
