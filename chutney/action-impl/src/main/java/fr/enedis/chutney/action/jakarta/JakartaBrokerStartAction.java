/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;

public class JakartaBrokerStartAction implements Action {

    private final Logger logger;
    private final FinallyActionRegistry finallyActionRegistry;
    private final String configurationUri;

    public JakartaBrokerStartAction(Logger logger,
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
            ActiveMQServer brokerService = ActiveMQServers.newActiveMQServer(new ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("broker", this.configurationUri));
            brokerService.start();
            logger.info("Started with configuration uri: " + this.configurationUri);
            createQuitFinallyAction(brokerService);
            return ActionExecutionResult.ok(toOutputs(brokerService));
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }

    private String defaultConfiguration() {
        return "tcp://localhost:61616";
    }

    private Map<String, Object> toOutputs(ActiveMQServer brokerService) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("jakartaBrokerService", brokerService);
        return outputs;
    }

    private void createQuitFinallyAction(ActiveMQServer brokerService) {
        finallyActionRegistry.registerFinallyAction(
            FinallyAction.Builder
                .forAction("jakarta-broker-stop", JakartaBrokerStartAction.class)
                .withInput("jakarta-broker-service", brokerService)
                .build()
        );
        logger.info("JmsBrokerStop finally action registered");
    }
}
