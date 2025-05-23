/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp;

import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.validation.Validator;
import java.util.List;
import java.util.Objects;
import org.apache.qpid.server.SystemLauncher;

public class QpidServerStopAction implements Action {

    private final Logger logger;
    private final SystemLauncher systemLauncher;

    public QpidServerStopAction(Logger logger, @Input("qpid-launcher") SystemLauncher systemLauncher) {
        this.logger = logger;
        this.systemLauncher = systemLauncher;
    }

    @Override
    public List<String> validateInputs() {
        Validator<SystemLauncher> systemValidation = of(systemLauncher)
            .validate(Objects::nonNull, "No qpid-launcher provided");
        return getErrorsFrom(systemValidation);
    }

    @Override
    public ActionExecutionResult execute() {
        logger.info("Call Qpid Server shutdown");
        systemLauncher.shutdown();
        return ActionExecutionResult.ok();
    }
}
