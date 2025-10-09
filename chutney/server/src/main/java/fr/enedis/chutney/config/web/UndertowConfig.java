/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.web;

import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_HTTP_INTERFACE_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_SSL_ENABLED_SPRING_VALUE;

import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"undertow-https-redirect"})
public class UndertowConfig {

    @Value(SERVER_PORT_SPRING_VALUE)
    private int securePort;

    @Value(SERVER_HTTP_INTERFACE_SPRING_VALUE)
    private String httpInterface;

    @Value(SERVER_SSL_ENABLED_SPRING_VALUE)
    private Boolean sslEnabled;

    @Bean
    public UndertowServletWebServerFactory servletWebServerFactory() {
        UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
        // Add http listener
        if (sslEnabled && securePort == 443) {
            factory.getBuilderCustomizers().add(builder -> builder.addHttpListener(80, httpInterface));
        }
        // Redirect rule to secure port
        factory.getDeploymentInfoCustomizers().add(deploymentInfo ->
            deploymentInfo.addSecurityConstraint(
                new SecurityConstraint()
                    .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/*"))
                    .setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL)
                    .setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT))
                .setConfidentialPortManager(
                    exchange -> securePort
                ));
        return factory;
    }
}
