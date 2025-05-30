/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.qpid.server.SystemLauncher;
import org.springframework.core.io.ClassPathResource;

public class QpidServerStartAction implements Action {

    private final Logger logger;
    private final FinallyActionRegistry finallyActionRegistry;
    private final String initialConfiguration;

    public QpidServerStartAction(Logger logger,
                               FinallyActionRegistry finallyActionRegistry,
                               @Input("init-config") String initialConfiguration) {
        this.logger = logger;
        this.finallyActionRegistry = finallyActionRegistry;
        this.initialConfiguration = Optional.ofNullable(initialConfiguration)
            .orElseGet(this::defaultConfiguration);
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            SystemLauncher systemLauncher = new SystemLauncher();
            logger.info("Try to start qpid server");
            systemLauncher.startup(createSystemConfig());
            createQuitFinallyAction(systemLauncher);
            return ActionExecutionResult.ok(toOutputs(systemLauncher));
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }

    private String defaultConfiguration() {
        try {
            return new ClassPathResource("fr/enedis/chutney/action/amqp/default_qpid.json").getURL().toExternalForm();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private Map<String, Object> createSystemConfig() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("type", "Memory");
        attributes.put("initialConfigurationLocation", initialConfiguration);
        attributes.put("startupLoggedToSystemOut", true);
        return attributes;
    }

    private Map<String, Object> toOutputs(SystemLauncher systemLauncher) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("qpidLauncher", systemLauncher);
        return outputs;
    }

    private void createQuitFinallyAction(SystemLauncher systemLauncher) {
        finallyActionRegistry.registerFinallyAction(
            FinallyAction.Builder
                .forAction("qpid-server-stop", QpidServerStartAction.class)
                .withInput("qpid-launcher", systemLauncher)
                .build()
        );
        logger.info("QpidServerStop finally action registered");
    }

}
