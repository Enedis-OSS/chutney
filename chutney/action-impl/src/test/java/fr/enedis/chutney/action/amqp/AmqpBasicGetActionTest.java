/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp;

import static fr.enedis.chutney.action.amqp.AmqpActionsTest.mockConnectionFactory;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.ActionExecutionResult.Status;
import fr.enedis.chutney.action.spi.injectable.Target;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import org.junit.jupiter.api.Test;

public class AmqpBasicGetActionTest {

    @Test
    public void basicGet_fails_when_no_message_is_available() {
        MockConnectionFactory mockConnectionFactory = new MockConnectionFactory();
        TestLogger logger = new TestLogger();
        Target target = TestTarget.TestTargetBuilder.builder()
            .withTargetId("rabbit")
            .withUrl("amqp://non_host:1234")
            .withProperty("user", "guest")
            .withProperty("password", "guest")
            .build();

        String queueName = mockConnectionFactory.newConnection().createChannel().queueDeclare().getQueue();

        Action amqpBasicGetAction = mockConnectionFactory(new AmqpBasicGetAction(target, queueName, logger), mockConnectionFactory);

        ActionExecutionResult actionExecutionResult = amqpBasicGetAction.execute();

        assertThat(actionExecutionResult.status).isEqualTo(Status.Failure);
        assertThat(logger.errors).containsOnly("No message available");
    }
}
