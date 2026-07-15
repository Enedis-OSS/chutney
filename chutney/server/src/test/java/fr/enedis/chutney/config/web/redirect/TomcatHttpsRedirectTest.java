/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.web.redirect;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import fr.enedis.chutney.config.web.TomcatHttpsRedirectConfig;
import fr.enedis.chutney.tools.SocketUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = TomcatHttpsRedirectTest.RedirectTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ActiveProfiles("https-redirect")
@TestPropertySource(properties = {
    "server.http.interface=127.0.0.1",
    "server.ssl.enabled=true",
    "server.ssl.key-store=classpath:security/https/server.jks",
    "server.ssl.key-store-password=server",
    "server.ssl.key-password=server",
    "server.ssl.trust-store=classpath:security/https/truststore.jks",
    "server.ssl.trust-store-password=truststore",
})
class TomcatHttpsRedirectTest {

    private static final int httpsPort = SocketUtils.findAvailableTcpPort();
    private static final int httpPort = SocketUtils.findAvailableTcpPort();

    @DynamicPropertySource
    static void dynamicPorts(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> httpsPort);
        registry.add("server.http.port", () -> httpPort);
    }

    private final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER) // we want to inspect the 301/302
        .build();

    @Test
    void should_redirect_http_to_https_on_configured_ports() throws Exception {
        var httpUrl = "http://127.0.0.1:" + httpPort + "/";

        var request = HttpRequest.newBuilder(URI.create(httpUrl)).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isIn(301, 302);
        var location = response.headers().firstValue("Location");
        assertThat(location).isPresent();
        assertThat(location.get()).startsWith("https://127.0.0.1:" + httpsPort + "/");
    }

    /**
     * Isolated context: Tomcat + SSL + {@link TomcatHttpsRedirectConfig} only.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration",
        "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
        "org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration",
        "org.springframework.boot.data.ldap.autoconfigure.LdapRepositoriesAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.ActuatorAutoConfiguration",
        "org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration",
        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
    })
    @Import(TomcatHttpsRedirectConfig.class)
    static class RedirectTestApplication {
    }
}
