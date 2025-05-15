/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion.compare;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;

public class CompareContainsAction implements CompareExecutor {

    @Override
    public ActionExecutionResult compare(Logger logger, String actual, String expected) {
        if (actual.contains(expected)) {
            logger.info("[" + actual + "] CONTAINS [" + expected + "]");
            return ActionExecutionResult.ok();
        } else {
            logger.error("[" + actual + "] NOT CONTAINS [" + expected + "]");
            return ActionExecutionResult.ko();
        }
    }
}
