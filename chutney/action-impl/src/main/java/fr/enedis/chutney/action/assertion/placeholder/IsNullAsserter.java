/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.placeholder;

import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Collection;

public class IsNullAsserter implements PlaceholderAsserter {

    private static final String IS_NULL = "$isNull";

    @Override
    public boolean canApply(String value) {
        return IS_NULL.equals(value);
    }

    @Override
    public boolean assertValue(Logger logger, Object actual, Object expected) {
        logger.info("Verify " + actual + " == null");
        if (actual == null) {
            return true;
        }

        if (actual instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        return false;
    }
}
