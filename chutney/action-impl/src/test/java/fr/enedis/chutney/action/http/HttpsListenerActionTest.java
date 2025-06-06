/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Failure;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.http.function.WireMockFunction;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.tools.SocketUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestTemplate;

public class HttpsListenerActionTest {

    private final int wireMockPort = SocketUtils.findAvailableTcpPort();

    private final Logger logger = new TestLogger();
    private final WireMockServer server = new WireMockServer(wireMockConfig().port(wireMockPort));

    @BeforeEach
    public void setUp() {
        server.start();

        server.stubFor(any(anyUrl())
            .willReturn(aResponse().withStatus(200))
        );
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/test.*", "/.*"})
    public void should_success_when_receive_1_expected_message(String listenedUri) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForLocation("http://localhost:" + wireMockPort + "/test?param=toto&param2=toto2", "fake request");

        Action action = new HttpsListenerAction(logger, server, listenedUri, "POST", "1");
        ActionExecutionResult executionResult = action.execute();

        assertThat(executionResult.status).isEqualTo(Success);
        List<LoggedRequest> requests = (List<LoggedRequest>) executionResult.outputs.get("requests");
        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().getBodyAsString()).isEqualTo("fake request");
        assertThat(WireMockFunction.wiremockQueryParams(requests.getFirst())).containsOnly(entry("param", "toto"), entry("param2", "toto2"));

    }

    @Test
    public void should_failed_when_not_received_expected_number_of_message_on_expected_url() {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForLocation("http://localhost:" + wireMockPort + "/toto", "fake request");

        Action action = new HttpsListenerAction(logger, server, "/test", "POST", "1");
        ActionExecutionResult executionResult = action.execute();

        assertThat(executionResult.status).isEqualTo(Failure);
    }
}
