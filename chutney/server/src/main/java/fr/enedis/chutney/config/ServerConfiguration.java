/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config;

import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfiguration.class);

    @Value(SERVER_PORT_SPRING_VALUE)
    int port;

    @PostConstruct
    public void logPort() throws UnknownHostException {
        LOGGER.debug("Starting server {} on {}", InetAddress.getLocalHost().getCanonicalHostName(), port);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }


    // TODO - To move in infra when it will not be used in domain (ScenarioExecutionEngineAsync)
    @Bean
    public ObjectMapper reportObjectMapper() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .findAndRegisterModules();
    }

}
