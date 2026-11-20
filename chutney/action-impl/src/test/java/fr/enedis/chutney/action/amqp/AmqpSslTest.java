/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Failure;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestFinallyActionRegistry;
import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.TestTarget.TestTargetBuilder;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.ActionExecutionResult.Status;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.SocketUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.qpid.server.SystemLauncher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AmqpSslTest {

    private static final String SERVER_KEYSTORE = resourcePath("/security/server.jks");
    private static final String CLIENT_KEYSTORE = resourcePath("/security/client.jks");
    private static final String CLIENT_KEYSTORE_WITH_KEY_PASSWORD = resourcePath("/security/client-with-keypwd.jks");
    private static final String TRUSTSTORE = resourcePath("/security/truststore.jks");
    private static final String KEYSTORE_FROM_ANOTHER_CA = resourcePath("/security/keystore-with-keypwd.jks");

    private static final String EXCHANGE = "amq.direct";

    private static SystemLauncher qpidServer;
    private static int sslPort;
    private static int mutualSslPort;
    private static int tls13OnlyPort;

    @BeforeAll
    static void startQpidServerWithSslPorts(@TempDir Path tempDir) throws IOException {
        sslPort = SocketUtils.freePortFromSystem();
        mutualSslPort = SocketUtils.freePortFromSystem();
        tls13OnlyPort = SocketUtils.freePortFromSystem();
        Path configuration = Files.writeString(tempDir.resolve("qpid_ssl.json"), qpidConfiguration());

        ActionExecutionResult result = new QpidServerStartAction(new TestLogger(), new TestFinallyActionRegistry(), configuration.toUri().toString()).execute();

        assertThat(result.status).isEqualTo(Success);
        qpidServer = (SystemLauncher) result.outputs.get("qpidLauncher");
    }

    @AfterAll
    static void stopQpidServer() {
        if (qpidServer != null) {
            qpidServer.shutdown();
        }
    }

    @Test
    void should_publish_and_get_message_over_ssl_with_trust_store() {
        Target target = sslTarget(sslPort)
            .withProperty("trustStore", TRUSTSTORE)
            .withProperty("trustStorePassword", "truststore")
            .build();
        String queue = "with_trust_store";

        assertThat(createQueue(target, queue)).isEqualTo(Success);
        assertThat(new AmqpBasicPublishAction(target, EXCHANGE, queue, Map.of(), Map.of(), "over ssl", new TestLogger()).execute().status).isEqualTo(Success);

        ActionExecutionResult getResult = new AmqpBasicGetAction(target, queue, new TestLogger()).execute();
        assertThat(getResult.status).isEqualTo(Success);
        assertThat(getResult.outputs.get("body")).isEqualTo("over ssl");
    }

    @Test
    void should_trust_any_server_certificate_without_trust_store() {
        Target target = sslTarget(sslPort).build();

        assertThat(createQueue(target, "without_trust_store")).isEqualTo(Success);
    }

    @Test
    void should_connect_over_ssl_using_addresses_property() {
        Target target = TestTargetBuilder.builder()
            .withTargetId("qpid")
            .withUrl("amqps://unknown_host:1234")
            .withProperty("addresses", "localhost:" + sslPort)
            .withProperty("user", "guest")
            .withProperty("password", "guest")
            .build();

        assertThat(createQueue(target, "with_addresses")).isEqualTo(Success);
    }

    @Test
    void should_fail_when_trust_store_does_not_trust_server_certificate() {
        Target target = sslTarget(sslPort)
            .withProperty("trustStore", KEYSTORE_FROM_ANOTHER_CA)
            .withProperty("trustStorePassword", "server")
            .build();

        assertThat(createQueue(target, "untrusted_server")).isEqualTo(Failure);
    }

    @Test
    void should_fail_on_ssl_port_without_amqps_scheme() {
        Target target = target("amqp", sslPort).build();

        assertThat(createQueue(target, "without_ssl")).isEqualTo(Failure);
    }

    @Test
    void should_authenticate_with_client_certificate_from_key_store() {
        Target target = sslTarget(mutualSslPort)
            .withProperty("keyStore", CLIENT_KEYSTORE)
            .withProperty("keyStorePassword", "client")
            .build();

        assertThat(createQueue(target, "with_key_store")).isEqualTo(Success);
    }

    @Test
    void should_fail_when_server_needs_client_certificate_and_no_key_store() {
        Target target = sslTarget(mutualSslPort).build();

        assertThat(createQueue(target, "without_key_store")).isEqualTo(Failure);
    }

    @Test
    void should_use_key_password_to_read_client_key() {
        Target target = sslTarget(mutualSslPort)
            .withProperty("keyStore", CLIENT_KEYSTORE_WITH_KEY_PASSWORD)
            .withProperty("keyStorePassword", "client")
            .withProperty("keyPassword", "key_pwd")
            .build();

        assertThat(createQueue(target, "with_key_password")).isEqualTo(Success);
    }

    @Test
    void should_fail_with_logged_error_when_key_password_is_missing_and_differs_from_key_store_password() {
        Target target = sslTarget(mutualSslPort)
            .withProperty("keyStore", CLIENT_KEYSTORE_WITH_KEY_PASSWORD)
            .withProperty("keyStorePassword", "client")
            .build();
        TestLogger logger = new TestLogger();

        ActionExecutionResult result = new AmqpCreateBoundTemporaryQueueAction(target, EXCHANGE, "key", "without_key_password", logger, new TestFinallyActionRegistry()).execute();

        assertThat(result.status).isEqualTo(Failure);
        assertThat(logger.errors).singleElement().asString().contains("SSL configuration error", "UnrecoverableKeyException");
    }

    @Test
    void should_fail_with_logged_error_when_ssl_protocol_is_unknown() {
        Target target = sslTarget(sslPort).withProperty("sslProtocol", "unknown").build();
        TestLogger logger = new TestLogger();

        ActionExecutionResult result = new AmqpCreateBoundTemporaryQueueAction(target, EXCHANGE, "key", "unknown_ssl_protocol", logger, new TestFinallyActionRegistry()).execute();

        assertThat(result.status).isEqualTo(Failure);
        assertThat(logger.errors).singleElement().asString().contains("SSL configuration error");
    }

    @Test
    void should_use_ssl_protocol_property() {
        Target tls13Target = sslTarget(tls13OnlyPort).withProperty("sslProtocol", "TLSv1.3").build();
        Target tls12Target = sslTarget(tls13OnlyPort).withProperty("sslProtocol", "TLSv1.2").build();

        assertThat(createQueue(tls13Target, "with_tls13")).isEqualTo(Success);
        assertThat(createQueue(tls12Target, "with_tls12")).isEqualTo(Failure);
    }

    private static Status createQueue(Target target, String queueName) {
        return new AmqpCreateBoundTemporaryQueueAction(target, EXCHANGE, queueName, queueName, new TestLogger(), new TestFinallyActionRegistry()).execute().status;
    }

    private static TestTargetBuilder sslTarget(int port) {
        return target("amqps", port);
    }

    private static TestTargetBuilder target(String scheme, int port) {
        return TestTarget.TestTargetBuilder.builder()
            .withTargetId("qpid")
            .withUrl(scheme + "://localhost:" + port)
            .withProperty("user", "guest")
            .withProperty("password", "guest");
    }

    private static String resourcePath(String resource) {
        return AmqpSslTest.class.getResource(resource).getPath();
    }

    private static String qpidConfiguration() {
        return """
            {
                "name": "Embedded Broker",
                "modelVersion": "7.0",
                "authenticationproviders": [
                    {
                        "name": "Hardcoded",
                        "type": "Plain",
                        "secureOnlyMechanisms": [],
                        "users": [{"name": "guest", "password": "guest", "type": "managed"}]
                    }
                ],
                "keystores": [
                    {"name": "serverKeyStore", "type": "FileKeyStore", "keyStoreType": "JKS", "storeUrl": "%s", "password": "server"}
                ],
                "truststores": [
                    {"name": "clientTrustStore", "type": "FileTrustStore", "trustStoreType": "JKS", "storeUrl": "%s", "password": "client"}
                ],
                "ports": [
                    {
                        "name": "AMQPS",
                        "port": "%d",
                        "protocols": ["AMQP_0_9", "AMQP_0_9_1"],
                        "transports": ["SSL"],
                        "keyStore": "serverKeyStore",
                        "authenticationProvider": "Hardcoded",
                        "virtualhostaliases": [{"name": "defaultAlias", "type": "defaultAlias"}]
                    },
                    {
                        "name": "AMQPS_MUTUAL",
                        "port": "%d",
                        "protocols": ["AMQP_0_9", "AMQP_0_9_1"],
                        "transports": ["SSL"],
                        "keyStore": "serverKeyStore",
                        "needClientAuth": true,
                        "trustStores": ["clientTrustStore"],
                        "authenticationProvider": "Hardcoded",
                        "virtualhostaliases": [{"name": "defaultAlias", "type": "defaultAlias"}]
                    },
                    {
                        "name": "AMQPS_TLS13_ONLY",
                        "port": "%d",
                        "protocols": ["AMQP_0_9", "AMQP_0_9_1"],
                        "transports": ["SSL"],
                        "keyStore": "serverKeyStore",
                        "context": {"qpid.security.tls.protocolAllowList": "TLSv1.3"},
                        "authenticationProvider": "Hardcoded",
                        "virtualhostaliases": [{"name": "defaultAlias", "type": "defaultAlias"}]
                    }
                ],
                "virtualhostnodes": [
                    {
                        "name": "default",
                        "type": "Memory",
                        "defaultVirtualHostNode": "true",
                        "virtualHostInitialConfiguration": "{\\"type\\": \\"Memory\\"}"
                    }
                ]
            }
            """.formatted(SERVER_KEYSTORE, CLIENT_KEYSTORE, sslPort, mutualSslPort, tls13OnlyPort);
    }
}
