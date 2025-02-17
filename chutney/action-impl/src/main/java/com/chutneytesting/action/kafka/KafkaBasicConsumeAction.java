/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.kafka;

import static com.chutneytesting.action.spi.validation.ActionValidatorsUtils.durationValidation;
import static com.chutneytesting.action.spi.validation.ActionValidatorsUtils.enumValidation;
import static com.chutneytesting.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static com.chutneytesting.action.spi.validation.ActionValidatorsUtils.targetValidation;
import static com.chutneytesting.action.spi.validation.Validator.getErrorsFrom;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_XML;

import com.chutneytesting.action.amqp.utils.JsonPathEvaluator;
import com.chutneytesting.action.function.XPathFunction;
import com.chutneytesting.action.spi.Action;
import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Input;
import com.chutneytesting.action.spi.injectable.Logger;
import com.chutneytesting.action.spi.injectable.Target;
import com.chutneytesting.action.spi.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

public class KafkaBasicConsumeAction implements Action {

    private final KafkaConsumerFactory kafkaConsumerFactory = new KafkaConsumerFactory();

    static final String OUTPUT_BODY = "body";
    static final String OUTPUT_BODY_HEADERS_KEY = "headers";
    static final String OUTPUT_BODY_PAYLOAD_KEY = "payload";
    static final String OUTPUT_HEADERS = "headers";
    static final String OUTPUT_PAYLOADS = "payloads";
    static final String OUTPUT_BODY_KEY_KEY = "key";
    static final String OUTPUT_KEYS = "keys";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String topic;
    private final Logger logger;
    private final Integer nbMessages;
    private final Map<String, String> properties;
    private final MimeType contentType;
    private final String timeout;
    private final String selector;
    private final String headerSelector;
    private final Target target;
    private final CountDownLatch countDownLatch;
    private final List<Map<String, Object>> consumedMessages = new ArrayList<>();
    private final String group;
    private final String ackMode;
    private final Boolean resetOffset;
    private MimeType recordContentType;

    public KafkaBasicConsumeAction(Target target,
                                   @Input("topic") String topic,
                                   @Input("group") String group,
                                   @Input("properties") Map<String, String> properties,
                                   @Input("nb-messages") Integer nbMessages,
                                   @Input("selector") String selector,
                                   @Input("header-selector") String headerSelector,
                                   @Input("content-type") String contentType,
                                   @Input("timeout") String timeout,
                                   @Input("ackMode") String ackMode,
                                   @Input("reset-offset") Boolean resetOffset,
                                   Logger logger) {
        this.topic = topic;
        this.nbMessages = defaultIfNull(nbMessages, 1);
        this.selector = selector;
        this.headerSelector = headerSelector;
        this.contentType = ofNullable(contentType).map(ct -> defaultIfEmpty(ct, APPLICATION_JSON_VALUE)).map(MimeTypeUtils::parseMimeType).orElse(APPLICATION_JSON);
        this.timeout = defaultIfEmpty(timeout, "60 sec");
        this.target = target;
        this.countDownLatch = new CountDownLatch(this.nbMessages > 0 ? this.nbMessages : 1);
        this.group = group;
        this.logger = logger;
        this.properties = ofNullable(properties).orElse(emptyMap());
        this.ackMode = ofNullable(ackMode)
            .or(() -> ofNullable(target).flatMap(t -> t.property("ackMode")))
            .orElse(ContainerProperties.AckMode.BATCH.name());
        this.resetOffset = ofNullable(resetOffset).orElse(false);
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(topic, "topic"),
            notBlankStringValidation(group, "group"),
            targetValidation(target),
            durationValidation(timeout, "timeout"),
            enumValidation(ContainerProperties.AckMode.class, ackMode, "ackMode")
        );
    }

    @Override
    public ActionExecutionResult execute() {
        var messageListenerContainer = kafkaConsumerFactory.create(
            target, topic, group, resetOffset, ackMode, createMessageListener(), new ListenerContainerErrorHandler(logger), properties
        );
        try {
            logger.info("Consuming message from topic " + topic);
            messageListenerContainer.start();
            countDownLatch.await(Duration.parse(timeout).toMilliseconds(), TimeUnit.MILLISECONDS);
            if (consumedMessages.size() != nbMessages) {
                logger.error("Unable to get the expected number of messages [" + nbMessages + "] during " + timeout + " from topic " + topic + ".");
                return ActionExecutionResult.ko();
            }
            logger.info("Consumed [" + nbMessages + "] Kafka Messages from topic " + topic);
            return ActionExecutionResult.ok(toOutputs());
        } catch (Exception e) {
            logger.error("An exception occurs when consuming a message to Kafka server: " + e.getMessage());
            return ActionExecutionResult.ko();
        } finally {
            messageListenerContainer.stop();
        }
    }

    private MessageListener<String, String> createMessageListener() {
        return record -> {
            if (countDownLatch.getCount() <= 0) {
                return;
            }
            final Map<String, Object> message = extractMessageFromRecord(record);
            if (applySelector(message) && applyHeaderSelector(message)) {
                addMessageToResultAndCountDown(message);
            }
        };
    }

    private boolean applySelector(Map<String, Object> message) {
        if (isBlank(selector)) {
            return true;
        } else if (message.get(OUTPUT_BODY_PAYLOAD_KEY) instanceof byte[]) {
            logger.info("Received binary message, ignoring payload selection");
            return true;
        }

        if (recordContentType.getSubtype().contains(APPLICATION_JSON.getSubtype())) {
            try {
                String messageAsString = OBJECT_MAPPER.writeValueAsString(message);
                return JsonPathEvaluator.evaluate(messageAsString, selector);
            } catch (Exception e) {
                logger.info("Received a message, however cannot read process it as json, ignoring payload selection : " + e.getMessage());
                return true;
            }
        } else if (recordContentType.getSubtype().contains(APPLICATION_XML.getSubtype())) {
            try {
                Object result = XPathFunction.xpath(String.valueOf(message.get(OUTPUT_BODY_PAYLOAD_KEY)), selector);
                return ofNullable(result).isPresent();
            } catch (Exception e) {
                logger.info("Received a message, however cannot read process it as xml, ignoring payload selection : " + e.getMessage());
                return true;
            }
        } else {
            if (message.get(OUTPUT_BODY_PAYLOAD_KEY) instanceof String msgString) {
                logger.info("Applying selector as text");
                return msgString.contains(selector);
            } else {
                logger.info("Received a message, however cannot read process it as text, ignoring payload selection");
                return true;
            }
        }
    }

    private boolean applyHeaderSelector(Map<String, Object> message) {
        if (isBlank(headerSelector)) {
            return true;
        }

        try {
            String messageAsString = OBJECT_MAPPER.writeValueAsString(message.get(OUTPUT_BODY_HEADERS_KEY));
            return JsonPathEvaluator.evaluate(messageAsString, headerSelector);
        } catch (Exception e) {
            logger.error("\"Received a message, however cannot process headers selection, Ignoring header selection");
            return true;
        }
    }

    private void addMessageToResultAndCountDown(Map<String, Object> message) {
        consumedMessages.add(message);
        countDownLatch.countDown();
    }

    private Object extractPayload(ConsumerRecord<?, ?> record) {
        if (recordContentType.getSubtype().contains(APPLICATION_JSON.getSubtype())) {
            if (record.value() instanceof String recordString) {
                try {
                    return OBJECT_MAPPER.readValue(recordString, Map.class);
                } catch (IOException e) {
                    logger.info("Received a message, however cannot read it as a Json value");
                }
            } else {
                logger.info("Received a message, however cannot read it as Json because it's not a string");
            }
        }
        return record.value();
    }

    private Map<String, Object> extractMessageFromRecord(ConsumerRecord<?, ?> record) {
        final Map<String, Object> message = new HashMap<>();
        final Map<String, Object> headers = extractHeaders(record);
        checkContentTypeHeader(headers);
        Object payload = extractPayload(record);
        message.put(OUTPUT_BODY_HEADERS_KEY, headers);
        message.put(OUTPUT_BODY_PAYLOAD_KEY, payload);
        message.put(OUTPUT_BODY_KEY_KEY, record.key());
        return message;
    }

    private Map<String, Object> extractHeaders(ConsumerRecord<?, ?> record) {
        var result = new HashMap<String, Object>();
        Stream<Header> distinctHeaders = Stream.of(record.headers().toArray()).distinct();
        distinctHeaders.forEach(header -> {
            String headerKey = header.key();
            if (result.containsKey(headerKey)) {
                Object headerValue = result.get(headerKey);
                if (headerValue instanceof String) {
                    var headerValueAsList = new ArrayList<>();
                    headerValueAsList.add(headerValue);
                    result.put(headerKey, headerValueAsList);
                }
                ((Collection<String>) result.get(headerKey)).add(new String(header.value(), UTF_8));
            } else {
                result.put(headerKey, new String(header.value(), UTF_8));
            }
        });
        return result;
    }

    private Map<String, Object> toOutputs() {
        Map<String, Object> results = new HashMap<>();
        results.put(OUTPUT_BODY, consumedMessages);
        results.put(OUTPUT_PAYLOADS, consumedMessages.stream().map(e -> e.get(OUTPUT_BODY_PAYLOAD_KEY)).collect(toList()));
        results.put(OUTPUT_HEADERS, consumedMessages.stream().map(e -> e.get(OUTPUT_BODY_HEADERS_KEY)).collect(toList()));
        results.put(OUTPUT_KEYS, consumedMessages.stream().map(e -> e.get(OUTPUT_BODY_KEY_KEY)).collect(toList()));
        return results;
    }

    private void checkContentTypeHeader(Map<String, Object> headers) {
        recordContentType = this.contentType;
        try {
            Optional<MimeType> contentType = headers.entrySet().stream()
                .filter(e -> e.getKey().replaceAll("[- ]", "").equalsIgnoreCase("contenttype"))
                .findAny()
                .map(Map.Entry::getValue)
                .map(Object::toString)
                .map(s -> s.replace("\"", ""))
                .map(MimeTypeUtils::parseMimeType);

            contentType.ifPresent(ct -> this.recordContentType = ct);
        } catch (Exception e) {
            logger.error("Cannot parse content type from message received:  " + e.getMessage());
        }
    }
}
