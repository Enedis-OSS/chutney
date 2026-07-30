/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config;

import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

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
        JsonMapper.Builder builder = JsonMapper.builder()
            .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .findAndAddModules();
        fr.enedis.chutney.config.web.ImmutablesJacksonMixins.register(builder);
        return builder.build();
    }

    @Bean
    ApplicationRunner listResources() {
        return args -> {
            var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
            for (var r : resolver.getResources("classpath*:**/*.ldif")) {
                System.out.println("found: " + r);
            }
        };
    }

}
