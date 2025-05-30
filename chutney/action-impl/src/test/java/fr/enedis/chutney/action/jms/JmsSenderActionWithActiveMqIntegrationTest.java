/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JmsSenderActionWithActiveMqIntegrationTest extends ActiveMQTestSupport {
    @Test
    public void failedSSL2WayAskWithOneWayProvided() throws Exception {

        String body = "messageBody";
        String destination = "dynamicQueues/testD";
        Map<String, String> headers = new HashMap<>();

        TestTarget target = TestTarget.TestTargetBuilder.builder()
            .withTargetId("id")
            .withUrl(needClientAuthConnector.getPublishableConnectString())
            .withProperty("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQSslInitialContextFactory")
            .withProperty("trustStore", "security/truststore.jks")
            .withProperty("trustStorePassword", "truststore")
            .build();

        Logger logger = mock(Logger.class);
        JmsSenderAction action = new JmsSenderAction(target, logger, destination, body, headers);

        action.execute();

        assertThat(needClientAuthConnector.getBrokerService().getTotalConnections()).isEqualTo(expectedTotalConnections.get());
    }

}
