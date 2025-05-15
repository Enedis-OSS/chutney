/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Objects;

// TODO replace by an assert on context compatible with language-expressions (spel, ognl, velocity, etc.)
@Deprecated
public class StringAssert implements Action {

    private final Logger logger;
    private final String actual;
    private final String expected;

    public StringAssert(Logger logger, @Input("document") String actual, @Input("expected") String expected) {
        this.logger = logger;
        this.actual = actual;
        this.expected = expected;
    }

    @Override
    public ActionExecutionResult execute() {
        if (!Objects.equals(actual, expected)) {
            logger.error("Expected value [" + expected + "], but found [" + actual + "]"
                    + " - Action string-assert is deprecated. Use assert-true instead."
            );
            return ActionExecutionResult.ko();
        } else {
            logger.info("Found expected value [" + actual + "]"
                   + " - Action string-assert is deprecated. Use assert-true instead."
            );
            return ActionExecutionResult.ok();
        }
    }

}
