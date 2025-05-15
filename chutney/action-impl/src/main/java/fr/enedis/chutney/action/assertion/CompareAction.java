/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import fr.enedis.chutney.action.assertion.compare.CompareActionFactory;
import fr.enedis.chutney.action.assertion.compare.CompareExecutor;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;

public class CompareAction implements Action {

    private final Logger logger;
    private final String actual;
    private final String expected;
    private final String mode;

    public CompareAction(Logger logger, @Input("actual") String actual, @Input("expected") String expected, @Input("mode") String mode) {
        this.logger = logger;
        this.actual = actual;
        this.expected = expected;
        this.mode = mode;
    }

    @Override
    public ActionExecutionResult execute() {
        CompareExecutor compareExecutor = CompareActionFactory.createCompareAction(mode);
        return compareExecutor.compare(logger, actual, expected);
    }
}
