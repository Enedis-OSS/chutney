/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka;

import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.validation.Validator;
import java.util.List;
import java.util.Objects;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

public class KafkaBrokerStopAction implements Action {

    private final Logger logger;
    private final EmbeddedKafkaBroker broker;

    public KafkaBrokerStopAction(Logger logger, @Input("broker") EmbeddedKafkaBroker broker) {
        this.logger = logger;
        this.broker = broker;
    }

    @Override
    public List<String> validateInputs() {
        Validator<EmbeddedKafkaBroker> embeddedKafkaBrokerValidation = of(broker)
            .validate(Objects::nonNull, "No broker provided");
        return getErrorsFrom(embeddedKafkaBrokerValidation);
    }

    @Override
    public ActionExecutionResult execute() {
        logger.info("Call Kafka broker shutdown");
        broker.destroy();
        return ActionExecutionResult.ok();
    }
}
