/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.context;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.durationValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SleepAction implements Action {

    private final Logger logger;
    private final String duration;

    public SleepAction(Logger logger, @Input("duration") String duration) {
        this.logger = logger;
        this.duration = duration;
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(durationValidation(duration, "duration"));
    }

    @Override
    public ActionExecutionResult execute() {
        logger.info("Start sleeping for " + duration);
        try {
            TimeUnit.MILLISECONDS.sleep(Duration.parse(duration).toMilliseconds());
        } catch (InterruptedException e) {
            logger.error("Stop sleeping due to Interruption signal");
            return ActionExecutionResult.ko();
        }
        logger.info("Stop sleeping for " + duration);
        return ActionExecutionResult.ok();
    }


}
