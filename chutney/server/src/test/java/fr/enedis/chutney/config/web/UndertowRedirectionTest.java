/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.web;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
    "server.port=443",
})
class UndertowRedirectionTest {
    private final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER) // we want to inspect the 301/302
        .build();

    @Test
    void should_redirect_http_80_to_https_443() throws Exception {
        var appNamePath = "/api/v1/info/appname";
        var httpUrl = "http://localhost" + appNamePath ;

        // Act
        var request = HttpRequest.newBuilder(URI.create(httpUrl)).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        // Assert
        assertThat(response.statusCode()).isIn(301, 302); // either is fine
        var location = response.headers().firstValue("Location");
        assertThat(location).isPresent();
        assertThat(location.get()).startsWith("https://localhost:443" + appNamePath);
    }
}
