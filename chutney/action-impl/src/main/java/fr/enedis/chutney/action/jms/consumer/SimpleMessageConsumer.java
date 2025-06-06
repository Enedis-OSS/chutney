/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms.consumer;

import java.util.Optional;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;

class SimpleMessageConsumer implements Consumer {

    private final MessageConsumer messageConsumer;
    private final long timeout;

    SimpleMessageConsumer(MessageConsumer messageConsumer, long timeout) {
        this.messageConsumer = messageConsumer;
        this.timeout = timeout;
    }

    @Override
    public Optional<Message> getMessage() throws JMSException {
        return Optional.ofNullable(messageConsumer.receive(timeout));
    }
}
