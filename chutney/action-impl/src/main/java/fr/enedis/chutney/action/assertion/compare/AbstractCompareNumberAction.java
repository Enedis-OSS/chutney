/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.compare;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.function.BiFunction;

public abstract class AbstractCompareNumberAction implements CompareExecutor {

    protected abstract BiFunction<Double, Double, Boolean> compareFunction();

    protected abstract String getFunctionName();

    protected abstract String getOppositeFunctionName();

    @Override
    public ActionExecutionResult compare(Logger logger, String actual, String expected) {

        Double actualD = parse(logger, actual);
        Double expectedD = parse(logger, expected);

        Boolean apply = compareFunction().apply(actualD, expectedD);
        if (apply) {
            logger.info("[" + actual + "] " + getFunctionName() + " [" + expected + "]");
            return ActionExecutionResult.ok();
        } else {
            logger.error("[" + actual + "] " + getOppositeFunctionName() + " [" + expected + "]");
            return ActionExecutionResult.ko();
        }
    }

    private Double parse(Logger logger, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.error("[" + value + "] is Not Numeric");
        }
        return Double.NaN;
    }
}
