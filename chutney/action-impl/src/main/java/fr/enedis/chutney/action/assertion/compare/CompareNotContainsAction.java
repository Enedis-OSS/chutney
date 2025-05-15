/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.compare;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;

public class CompareNotContainsAction implements CompareExecutor {

    @Override
    public ActionExecutionResult compare(Logger logger, String actual, String expected) {
        if (actual.contains(expected)) {
            logger.error("[" + actual + "] CONTAINS [" + expected + "]");
            return ActionExecutionResult.ko();
        } else {
            logger.info("[" + actual + "] NOT CONTAINS [" + expected + "]");
            return ActionExecutionResult.ok();
        }
    }
}
