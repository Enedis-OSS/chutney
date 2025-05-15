/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta;

import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.validation.Validator;
import java.util.List;
import java.util.Objects;
import org.apache.activemq.artemis.core.server.ActiveMQServer;

public class JakartaBrokerStopAction implements Action {

    private final Logger logger;
    private final ActiveMQServer brokerService;

    public JakartaBrokerStopAction(Logger logger, @Input("jakarta-broker-service") ActiveMQServer brokerService) {
        this.logger = logger;
        this.brokerService = brokerService;
    }

    @Override
    public List<String> validateInputs() {
        Validator<ActiveMQServer> jmsBrokerValidation = of(brokerService)
            .validate(Objects::nonNull, "No jakarta-broker-service provided");
        return getErrorsFrom(jmsBrokerValidation);
    }

    @Override
    public ActionExecutionResult execute() {
        logger.info("Call jms broker shutdown");
        try {
            brokerService.stop();
            return ActionExecutionResult.ok();
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }
}
