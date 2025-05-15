/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

public class JmsBrokerStartAction implements Action {

    private final Logger logger;
    private final FinallyActionRegistry finallyActionRegistry;
    private final String configurationUri;

    public JmsBrokerStartAction(Logger logger,
                              FinallyActionRegistry finallyActionRegistry,
                              @Input("config-uri") String configUri) {
        this.logger = logger;
        this.finallyActionRegistry = finallyActionRegistry;
        this.configurationUri = Optional.ofNullable(configUri)
            .orElseGet(this::defaultConfiguration);
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            BrokerService brokerService = BrokerFactory.createBroker(configurationUri);
            logger.info("Try to start jms broker");
            brokerService.start();
            createQuitFinallyAction(brokerService);
            return ActionExecutionResult.ok(toOutputs(brokerService));
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }

    private String defaultConfiguration() {
        return "broker:(tcp://localhost:61616)?useJmx=false&persistent=false";
    }

    private Map<String, Object> toOutputs(BrokerService brokerService) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("jmsBrokerService", brokerService);
        return outputs;
    }

    private void createQuitFinallyAction(BrokerService brokerService) {
        finallyActionRegistry.registerFinallyAction(
            FinallyAction.Builder
                .forAction("jms-broker-stop", JmsBrokerStartAction.class)
                .withInput("jms-broker-service", brokerService)
                .build()
        );
        logger.info("JmsBrokerStop finally action registered");
    }

}
