/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms;

import static fr.enedis.chutney.action.jms.JmsActionParameter.DESTINATION;
import static fr.enedis.chutney.action.jms.JmsActionParameter.TIMEOUT;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.durationValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import fr.enedis.chutney.action.jms.consumer.Consumer;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class JmsCleanQueueAction implements Action {

    private final Target target;
    private final Logger logger;

    private final JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory();
    private final String destination;
    private final String timeout;

    public JmsCleanQueueAction(Target target, Logger logger, @Input(DESTINATION) String destination, @Input(TIMEOUT) String timeout) {
        this.target = target;
        this.logger = logger;
        this.destination = destination;
        this.timeout = defaultIfEmpty(timeout, "500 ms");
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(destination, "destination"),
            durationValidation(timeout, "timeout"),
            targetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {

        try (CloseableResource<Consumer> consumer = jmsConnectionFactory.createConsumer(target, destination, timeout)) {
            int removedMessages = 0;
            Optional<Message> message;
            while ((message = consumer.getResource().getMessage()).isPresent()) {
                displayMessageContent(logger, message.get());
                removedMessages++;
            }

            logger.info("Removed " + removedMessages + " messages");
            return ActionExecutionResult.ok();
        } catch (JMSException | UncheckedJmsException e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }

    private void displayMessageContent(Logger logger, Message message) throws JMSException {
        Map<String, String> properties = propertiesToMap(message);

        final String body;
        if (message instanceof TextMessage textMessage) {
            body = textMessage.getText();
        } else {
            body = "";
        }
        logger.info("Removed: " + properties + " " + body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> propertiesToMap(Message message) throws JMSException {
        Enumeration<String> propertyNames = message.getPropertyNames();
        Map<String, String> properties = new LinkedHashMap<>();
        while (propertyNames.hasMoreElements()) {
            String propertyName = propertyNames.nextElement();
            properties.put(propertyName, String.valueOf(message.getObjectProperty(propertyName)));
        }
        return properties;
    }
}
