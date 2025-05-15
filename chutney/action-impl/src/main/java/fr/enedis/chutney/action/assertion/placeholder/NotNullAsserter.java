/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.placeholder;

import fr.enedis.chutney.action.spi.injectable.Logger;

public class NotNullAsserter implements PlaceholderAsserter {

    private static final String IS_NOTNULL = "$isNotNull";

    @Override
    public boolean canApply(String value) {
        return IS_NOTNULL.equals(value);
    }

    @Override
    public boolean assertValue(Logger logger, Object actual, Object expected) {
        logger.info("Verify " + actual + " != null");
        return actual != null;
    }

}
