/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static java.util.stream.Collectors.joining;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class AmqpBasicPublishAction implements Action {

    private static final String CONTENT_TYPE = "content_type";

    private final ConnectionFactoryFactory connectionFactoryFactory = new ConnectionFactoryFactory();

    private final Target target;
    private final String exchangeName;
    private final String routingKey;
    private final Map<String, Object> headers;
    private final Map<String, String> properties;
    private final String payload;
    private final Logger logger;

    public AmqpBasicPublishAction(Target target,
                                @Input("exchange-name") String exchangeName,
                                @Input("routing-key") String routingKey,
                                @Input("headers") Map<String, Object> headers,
                                @Input("properties") Map<String, String> properties,
                                @Input("payload") String payload,
                                Logger logger) {
        this.target = target;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.properties = properties != null ? properties : Collections.emptyMap();
        this.payload = payload;
        this.logger = logger;
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(exchangeName, "exchange-name"),
            notBlankStringValidation(payload, "payload"),
            targetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try (Connection connection = connectionFactoryFactory.newConnection(target);
             Channel channel = connection.createChannel()) {

            BasicProperties basicProperties = buildProperties();
            channel.basicPublish(exchangeName, routingKey, basicProperties, payload.getBytes());
            logger.info("Published AMQP Message on " + exchangeName + " with routing key: " + routingKey);
            return ActionExecutionResult.ok(outputs(basicProperties, payload));
        } catch (TimeoutException | IOException e) {
            logger.error("Unable to establish connection to RabbitMQ: " + e.getMessage());
            return ActionExecutionResult.ko();
        }
    }

    public Map<String, Object> outputs(BasicProperties basicProperties, String payload) {
        Map<String, Object> results = new HashMap<>();
        results.put("payload", payload);
        results.put("headers", basicProperties.getHeaders().entrySet().stream()
            .map(Map.Entry::toString)
            .collect(joining(";", "[", "]"))
        );
        return results;
    }

    private BasicProperties buildProperties() {
        Builder basicPropertiesBuilder = new Builder().appId("testing-app");
        if (properties.containsKey(CONTENT_TYPE)) {
            basicPropertiesBuilder.contentType(properties.get(CONTENT_TYPE));
        }
        basicPropertiesBuilder.headers(headers);
        return basicPropertiesBuilder.build();
    }
}
