/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;

public class HttpsServerStartAction implements Action {

    private static final int DEFAULT_HTTPS_PORT = 8443;

    private final Logger logger;
    private final FinallyActionRegistry finallyActionRegistry;
    private final int port;
    private final Optional<String> trustStorePath;
    private final Optional<String> trustStorePassword;
    private final Optional<String> keyStorePath;
    private final Optional<String> keyStorePassword;
    private final Optional<String> keyPassword;

    public HttpsServerStartAction(Logger logger,
                                FinallyActionRegistry finallyActionRegistry,
                                @Input("port") String port,
                                @Input("truststore-path") String trustStorePath,
                                @Input("truststore-password") String trustStorePassword,
                                @Input("keystore-path") String keyStorePath,
                                @Input("keystore-password") String keyStorePassword,
                                @Input("key-password") String keyPassword) {
        this.logger = logger;
        this.finallyActionRegistry = finallyActionRegistry;
        this.port = NumberUtils.toInt(port, DEFAULT_HTTPS_PORT);
        this.trustStorePath = ofNullable(trustStorePath);
        this.trustStorePassword = ofNullable(trustStorePassword);
        this.keyStorePath = ofNullable(keyStorePath);
        this.keyStorePassword = ofNullable(keyStorePassword);
        this.keyPassword = ofNullable(keyPassword);
    }

    @Override
    public ActionExecutionResult execute() {
        WireMockConfiguration wireMockConfiguration = wireMockConfig()
            .dynamicPort()
            .httpsPort(port)
            .containerThreads(8)
            .asynchronousResponseThreads(1)
            .jettyAcceptors(1);

        trustStorePath.ifPresent(ts -> wireMockConfiguration
            .needClientAuth(true)
            .trustStorePath(trustStorePath.orElse(""))
            .trustStorePassword(trustStorePassword.orElse(""))
        );

        // add keystore path and pwd if present
        keyStorePath.ifPresent(s -> wireMockConfiguration
            .keystorePath(s)
            .keystorePassword(keyStorePassword.orElse(""))
            .keyManagerPassword(keyPassword.orElse(""))
        );
        WireMockServer wireMockServer = new WireMockServer(wireMockConfiguration);
        logger.info("Try to start https server on port " + port);
        wireMockServer.start();

        wireMockServer.stubFor(any(anyUrl())
            .willReturn(aResponse().withStatus(200))
        );

        createQuitFinallyAction(wireMockServer);
        return ActionExecutionResult.ok(toOutputs(wireMockServer));
    }


    private Map<String, Object> toOutputs(WireMockServer httpsServer) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("httpsServer", httpsServer);
        return outputs;
    }

    private void createQuitFinallyAction(WireMockServer httpsServer) {
        finallyActionRegistry.registerFinallyAction(
            FinallyAction.Builder
                .forAction("https-server-stop", HttpsServerStartAction.class)
                .withInput("https-server", httpsServer)
                .build()
        );
        logger.info("HttpsServerStop finally action registered");
    }

}
