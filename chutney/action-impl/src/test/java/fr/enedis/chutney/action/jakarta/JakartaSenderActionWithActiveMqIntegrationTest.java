/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JakartaSenderActionWithActiveMqIntegrationTest extends ActiveMQTestSupport {
    @Test
    public void failedSSL2WayAskWithOneWayProvided() {
        String body = "messageBody";
        String destination = "dynamicQueues/testD";
        Map<String, String> headers = new HashMap<>();

        TestTarget target = TestTarget.TestTargetBuilder.builder()
            .withTargetId("id")
            .withUrl(serverUri() + "?" +
                "sslEnabled=true" +
                "&keyStorePath=" + keyStorePath +
                "&keyStorePassword=" + keyStorePassword +
                "&trustStorePath=" + trustStorePath +
                "&trustStorePassword" + trustStorePassword +
                "&verifyHost=false"
            )
            .withProperty("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory")
            .build();

        Logger logger = mock(Logger.class);
        JakartaSenderAction action = new JakartaSenderAction(target, logger, destination, body, headers);

        ActionExecutionResult result = action.execute();

        assertThat(result.status).isEqualTo(Success);

        JakartaListenerAction jmsListenerAction = new JakartaListenerAction(target, logger, destination, "2 sec", null, null, null);
        result = jmsListenerAction.execute();
        assertThat(result.status).isEqualTo(Success);
        assertThat(result.outputs.get("textMessage")).isEqualTo("messageBody");
    }
}
