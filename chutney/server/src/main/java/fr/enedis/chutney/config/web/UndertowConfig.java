/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.web;

import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_HTTP_INTERFACE_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;
import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_SSL_ENABLED_SPRING_VALUE;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"https-redirect"})
public class UndertowConfig {

    @Value(SERVER_PORT_SPRING_VALUE)
    private int securePort;

    @Value(SERVER_HTTP_INTERFACE_SPRING_VALUE)
    private String httpInterface;

    @Value(SERVER_SSL_ENABLED_SPRING_VALUE)
    private Boolean sslEnabled;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpsRedirectCustomizer() {
        return factory -> {
            if (sslEnabled && securePort == 443) {
                Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                httpConnector.setPort(80);
                httpConnector.setRedirectPort(443);
                httpConnector.setProperty("address", httpInterface);
                factory.addAdditionalTomcatConnectors(httpConnector);

                factory.addContextCustomizers(context -> {
                    SecurityConstraint constraint = new SecurityConstraint();
                    constraint.setUserConstraint("CONFIDENTIAL");
                    SecurityCollection collection = new SecurityCollection();
                    collection.addPattern("/*");
                    constraint.addCollection(collection);
                    context.addConstraint(constraint);
                });
            }
        };
    }
}
