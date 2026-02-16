/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Target;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class DefaultMongoDatabaseFactoryTest {

    private static final TestLogger logger = new TestLogger();
    private static final String CLIENT_TRUSTSTORE_PATH = "/mongo/certs/client.truststore.jks";
    private static final String CLIENT_KEYSTORE_PATH = "/mongo/certs/client.keystore.jks";
    private static String TRUSTSTORE_JKS;
    private static String KEYSTORE_JKS;
    private final String STORE_PASSWORD = "server";
    private final String DB_NAME = "local";


    @BeforeAll
    static void beforeAll() {
        try {
            TRUSTSTORE_JKS = Paths.get(DefaultMongoDatabaseFactoryTest.class.getResource(CLIENT_TRUSTSTORE_PATH).toURI()).toString();
            KEYSTORE_JKS = Paths.get(DefaultMongoDatabaseFactoryTest.class.getResource(CLIENT_KEYSTORE_PATH).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_configure_mongo_client_settings_using_target_properties() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withTargetId("mongo")
            .withUrl("mongodb://localhost:27017")
            .withProperty("databaseName", DB_NAME)
            .withProperty("username", "username")
            .withProperty("password", "password")
            .withProperty("keyStore", KEYSTORE_JKS)
            .withProperty("trustStore", TRUSTSTORE_JKS)
            .withProperty("trustStorePassword", STORE_PASSWORD)
            .withProperty("keyStorePassword", STORE_PASSWORD)
            .withProperty("connectionOptions.authMechanism", AuthenticationMechanism.SCRAM_SHA_1.getMechanismName())
            .withProperty("connectionOptions.appName", "mongoFactoryTest")
            .withProperty("connectionOptions.proxyHost", "testProxyHost")
            .withProperty("connectionOptions.proxyPort", "2222")
            .build();
        DefaultMongoDatabaseFactory factory = new DefaultMongoDatabaseFactory();

        MongoClient mockClient = mock(MongoClient.class);
        try (MockedStatic<MongoClients> mocked = Mockito.mockStatic(MongoClients.class)) {
            ArgumentCaptor<MongoClientSettings> settingsCap = ArgumentCaptor.forClass(MongoClientSettings.class);
            mocked.when(() -> MongoClients.create(settingsCap.capture())).thenReturn(mockClient);

            try (var ignored = factory.create(target)) {
                MongoClientSettings settings = settingsCap.getValue();
                assertThat(settings.getApplicationName()).isEqualTo("mongoFactoryTest");
                assertThat(settings.getSslSettings().isEnabled()).isTrue();
                assertThat(settings.getCredential()).isNotNull();
                assertThat(settings.getCredential().getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.SCRAM_SHA_1);
                assertThat(settings.getCredential().getUserName()).isEqualTo("username");
                assertThat(settings.getCredential().getPassword()).isEqualTo("password".toCharArray());
                assertThat(settings.getSocketSettings().getProxySettings()).isNotNull();
                assertThat(settings.getSocketSettings().getProxySettings().getHost()).isEqualTo("testProxyHost");
                assertThat(settings.getSocketSettings().getProxySettings().getPort()).isEqualTo(2222);
                verify(mockClient).getDatabase(DB_NAME);
            }
        }
    }

    @Test
    public void should_connect_using_x509_authentication() {
        GenericContainer<?> mongoContainer = new GenericContainer<>(DockerImageName.parse("mongo:latest"))
            .withExposedPorts(27017)
            .withCommand("--config /etc/mongod.conf")
            .withCopyFileToContainer(MountableFile.forClasspathResource("/mongo/mongod.conf"), "/etc/mongod.conf")
            .withCopyFileToContainer(MountableFile.forClasspathResource("/mongo/certs/server.pem"), "/etc/ssl/server.pem")
            .withCopyFileToContainer(MountableFile.forClasspathResource("/mongo/certs/ca.pem"), "/etc/ssl/ca.pem")
            .withCopyFileToContainer(MountableFile.forClasspathResource("/mongo/init-users.sh"), "/docker-entrypoint-initdb.d/init-users.sh");

        mongoContainer.start();

        Target mongoTarget = TestTarget.TestTargetBuilder.builder()
            .withTargetId("mongo")
            .withUrl("mongodb://" + mongoContainer.getHost() + ":" + mongoContainer.getMappedPort(27017))
            .withProperty("databaseName", DB_NAME)
            .withProperty("connectionOptions.authMechanism", AuthenticationMechanism.MONGODB_X509.getMechanismName())
            .withProperty("keyStore", KEYSTORE_JKS)
            .withProperty("trustStore", TRUSTSTORE_JKS)
            .withProperty("trustStorePassword", STORE_PASSWORD)
            .withProperty("keyStorePassword", STORE_PASSWORD)
            .build();

        MongoListAction action = new MongoListAction(mongoTarget, logger);
        ActionExecutionResult result = action.execute();


        assertThat(result.outputs)
            .extractingByKey("collectionNames")
            .asInstanceOf(InstanceOfAssertFactories.list(String.class))
            .isNotEmpty();
    }
}
