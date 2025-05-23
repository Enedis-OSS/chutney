/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.action.jakarta.consumer.bodySelector.BodySelector;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.QueueBrowser;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SelectedMessageConsumerTest {

    @Test
    public void browsing_stops_when_reaching_max_depth() throws JMSException {
        QueueBrowser browser = mock(QueueBrowser.class, RETURNS_DEEP_STUBS);
        when(browser.getEnumeration().hasMoreElements()).thenReturn(true); // infinite enumeration
        when(browser.getEnumeration().nextElement()).thenReturn(mock(Message.class));
        BodySelector bodySelector = m -> false; // never matches
        SelectedMessageConsumer selectedMessageConsumer = new SelectedMessageConsumer(browser, bodySelector, 3);

        Optional<Message> message = selectedMessageConsumer.getMessage();

        assertThat(message).isEmpty();
        verify(browser.getEnumeration(), times(3)).nextElement();
    }

    @Test
    public void browsing_stops_when_there_is_no_more_messages() throws JMSException {
        QueueBrowser browser = mock(QueueBrowser.class, RETURNS_DEEP_STUBS);
        when(browser.getEnumeration().hasMoreElements()).thenReturn(false); // empty enumeration
        BodySelector bodySelector = m -> true; // always matches
        SelectedMessageConsumer selectedMessageConsumer = new SelectedMessageConsumer(browser, bodySelector, 3);

        Optional<Message> message = selectedMessageConsumer.getMessage();

        assertThat(message).isEmpty();
        verify(browser.getEnumeration(), times(0)).nextElement();
    }

    @Test
    public void browsing_stops_when_matching() throws JMSException {
        QueueBrowser browser = mock(QueueBrowser.class, RETURNS_DEEP_STUBS);
        when(browser.getEnumeration().hasMoreElements()).thenReturn(true); // infinite enumeration
        when(browser.getEnumeration().nextElement()).thenReturn(mock(Message.class));
        BodySelector bodySelector = m -> true; // always matches
        SelectedMessageConsumer selectedMessageConsumer = new SelectedMessageConsumer(browser, bodySelector, 3);

        Optional<Message> message = selectedMessageConsumer.getMessage();

        assertThat(message).isPresent();
        verify(browser.getEnumeration(), times(1)).nextElement();
    }
}
