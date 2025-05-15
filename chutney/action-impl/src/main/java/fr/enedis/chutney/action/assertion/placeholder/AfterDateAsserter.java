/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.placeholder;

import fr.enedis.chutney.action.spi.injectable.Logger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AfterDateAsserter extends GuardedPlaceholderAsserter {

    private static final String LOCAL_DATETIME_AFTER = "$isAfterDate:";

    public AfterDateAsserter(Guard... guards) {
        super(guards);
    }

    @Override
    public boolean canApply(String value) {
        return value.startsWith(LOCAL_DATETIME_AFTER);
    }

    @Override
    public boolean assertGuardedValue(Logger logger, Object actual, Object expected) {
        String date = expected.toString().substring(LOCAL_DATETIME_AFTER.length());
        try {
            ZonedDateTime expectedDate = ZonedDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime actualDate = ZonedDateTime.parse(actual.toString(), DateTimeFormatter.ISO_DATE_TIME);
            logger.info("Verify " + actualDate + " isAfter " + expectedDate);
            return actualDate.isAfter(expectedDate);
        } catch (DateTimeParseException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

}
