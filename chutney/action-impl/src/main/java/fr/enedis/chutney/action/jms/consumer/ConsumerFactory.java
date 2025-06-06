/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms.consumer;

import static fr.enedis.chutney.action.spi.time.Duration.parseToMs;

import fr.enedis.chutney.action.jms.consumer.bodySelector.BodySelector;
import fr.enedis.chutney.action.jms.consumer.bodySelector.BodySelectorFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

public class ConsumerFactory {

    private final BodySelectorFactory bodySelectorFactory = new BodySelectorFactory();
    private final String bodySelector;
    private final String selector;
    private final String timeout;
    private final Integer browserMaxDepth;

    public ConsumerFactory(String bodySelector, String selector, String timeout, int browserMaxDepth) {
        this.bodySelector = bodySelector;
        this.selector = selector;
        this.timeout = timeout;
        this.browserMaxDepth = browserMaxDepth;
    }

    public Consumer build(Session session, Destination destination) throws JMSException {
        final Consumer consumer;
        if (bodySelector == null || bodySelector.isEmpty()) {
            MessageConsumer messageConsumer = session.createConsumer(destination, selector);
            consumer = new SimpleMessageConsumer(messageConsumer, (int) parseToMs(timeout));
        } else {
            QueueBrowser browser = session.createBrowser((Queue) destination, selector);
            BodySelector bodySelectorBuild = bodySelectorFactory.build(bodySelector);
            consumer = new SelectedMessageConsumer(browser, bodySelectorBuild, browserMaxDepth);
        }
        return consumer;
    }
}
