/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.compare;

import fr.enedis.chutney.action.spi.injectable.Logger;

public class Parser {

     static double actualDouble;
     static double expectedDouble;

    public static boolean isParsableFrom(Logger logger, String actual, String expected) {

        try {
            actualDouble = Double.parseDouble(actual);
        } catch (NumberFormatException nfe) {
            logger.error("[" + actual + "] is Not Numeric");
        }

        try {
            expectedDouble = Double.parseDouble(expected);
        } catch (NumberFormatException nfe) {
            logger.error("[" + expected + "] is Not Numeric");
            return false;
        }

        return true;
    }
}
