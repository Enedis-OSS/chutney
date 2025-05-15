/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.compare;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Objects;

public class CompareNotEqualsAction implements CompareExecutor {

    @Override
    public ActionExecutionResult compare(Logger logger, String actual, String expected) {
        if (Objects.equals(actual, expected)) {
            logger.error("[" + expected + "]" + " EQUALS " + "[" + actual+"]");
            return ActionExecutionResult.ko();
        } else {
            logger.info("[" + expected + "]" + " NOT EQUALS " + "[" + actual+"]");
            return ActionExecutionResult.ok();
        }
    }
}
