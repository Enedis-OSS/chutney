/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaBasicPublishAction implements Action {

    private final ChutneyKafkaProducerFactory producerFactory = new ChutneyKafkaProducerFactory();

    private final Target target;
    private final String topic;
    private final Map<String, String> headers;
    private final Object payload;
    private final Map<String, String> producerKafkaConfig;
    private final String key;
    private final Logger logger;

    public KafkaBasicPublishAction(Target target,
                                   @Input("topic") String topic,
                                   @Input("headers") Map<String, String> headers,
                                   @Input("payload") Object payload,
                                   @Input("properties") Map<String, String> producerKafkaConfig,
                                   @Input("key") String key,
                                   Logger logger) {
        this.target = target;
        this.topic = topic;
        this.headers = headers != null ? headers : emptyMap();
        this.payload = payload;
        this.key = key;
        this.producerKafkaConfig = ofNullable(producerKafkaConfig).orElse(emptyMap());
        this.logger = logger;
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(topic, "topic"),
            of(payload).validate(Objects::nonNull, "No payload provided"),
            targetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            List<Header> recordHeaders = headers.entrySet().stream()
                .map(it -> new RecordHeader(it.getKey(), it.getValue().getBytes()))
                .collect(Collectors.toList());

            logger.info("sending message to topic=" + topic);
            ProducerRecord<String, ?> producerRecord = new ProducerRecord<>(topic, null, key, payload, recordHeaders);

            KafkaTemplate<String, ?> kafkaTemplate = producerFactory.create(target, producerKafkaConfig);
            kafkaTemplate.send((ProducerRecord) producerRecord).get(5, SECONDS);

            logger.info("Published Kafka Message on topic " + topic + (key != null ? " with key " + key : ""));
            return ActionExecutionResult.ok(toOutputs(headers, payload));
        } catch (Exception e) {
            logger.error("An exception occurs when sending a message to Kafka server: " + e.getMessage());
            return ActionExecutionResult.ko();
        } finally {
            try {
                producerFactory.destroy();
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    private Map<String, Object> toOutputs(Map<String, String> headers, Object payload) {
        Map<String, Object> results = new HashMap<>();
        results.put("payload", payload);
        results.put("headers", headers.entrySet().stream()
            .map(Map.Entry::toString)
            .collect(joining(";", "[", "]"))
        );
        return results;
    }
}
