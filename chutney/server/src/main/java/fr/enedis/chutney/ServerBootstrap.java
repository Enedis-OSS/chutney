/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import fr.enedis.chutney.server.core.domain.execution.history.ExecutionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Located by the <b>spring-boot-maven-plugin</b> Maven plugin.
 */
@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration",
    "org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration",
    "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
    "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration"
})
public class ServerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerBootstrap.class);

    public static void main(String... args) {
        setUpJULSL4JBridge();
        final ConfigurableApplicationContext context = start(args);
        cleanApplicationState(context);
    }

    private static void setUpJULSL4JBridge() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static ConfigurableApplicationContext start(String... args) {
        SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(ServerBootstrap.class)
            .registerShutdownHook(true)
            .bannerMode(Mode.OFF);

        return appBuilder.build().run(args);
    }

    private static void cleanApplicationState(ConfigurableApplicationContext context) {
        int staleExecutionCount = context.getBean(ExecutionHistoryRepository.class).setAllRunningExecutionsToKO();
        LOGGER.info("Starting with " + staleExecutionCount + " unfinished executions");
    }
}
