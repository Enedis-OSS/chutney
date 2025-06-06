/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

public class ConsumerFactoryTest {

    @Test
    public void building_consumer_without_body_selector_returns_a_simple_consumer() throws JMSException {
        ConsumerFactory consumerFactory = new ConsumerFactory(null, null, "1 sec", 0);

        Session session = mock(Session.class);
        Consumer consumer = consumerFactory.build(session, null);

        assertThat(consumer).isInstanceOf(SimpleMessageConsumer.class);
    }

    @Test
    public void building_consumer_with_body_selector_returns_a_selected_consumer() throws JMSException {
        ConsumerFactory consumerFactory = new ConsumerFactory("XPATH '/test'", null, "1 sec", 0);


        Session session = mock(Session.class);
        Consumer consumer = consumerFactory.build(session, null);

        assertThat(consumer).isInstanceOf(SelectedMessageConsumer.class);
    }
}
