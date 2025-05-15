/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka;

import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_BODY;
import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_BODY_HEADERS_KEY;
import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_BODY_KEY_KEY;
import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_BODY_PAYLOAD_KEY;
import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_HEADERS;
import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_KEYS;
import static fr.enedis.chutney.action.kafka.KafkaBasicConsumeAction.OUTPUT_PAYLOADS;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Failure;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.http.HttpsServerStartActionTest;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Target;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

public class KafkaBasicConsumeActionIntegrationTest {

    private final String GROUP = "mygroup";
    private String uniqueTopic;

    private final static EmbeddedKafkaBroker embeddedKafkaBroker = new EmbeddedKafkaZKBroker(1, true);
    private static Producer<Integer, String> producer;
    private final TestTarget.TestTargetBuilder targetBuilder;

    private TestLogger logger;

    public KafkaBasicConsumeActionIntegrationTest() {
        embeddedKafkaBroker.afterPropertiesSet();
        String brokerPath = embeddedKafkaBroker.getBrokersAsString();
        producer = createProducer(brokerPath);
        targetBuilder = TestTarget.TestTargetBuilder.builder().withTargetId("kafka").withUrl("tcp://" + brokerPath);
    }

    @BeforeEach
    public void before() {
        logger = new TestLogger();
        uniqueTopic = UUID.randomUUID().toString();
    }

    @AfterAll
    public static void afterAll() {
        producer.close();
        embeddedKafkaBroker.destroy();
    }

    @Test
    void consume_message_from_broker() {
        // Given
        producer.send(new ProducerRecord<>(uniqueTopic, 123, "my-test-value"));

        Map<String, String> props = new HashMap<>();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name().toLowerCase());

        // When
        Action consumeAction = getKafkaBasicConsumeAction(targetBuilder.build(), props, false);
        ActionExecutionResult actionExecutionResult = consumeAction.execute();

        // Then
        assertThat(actionExecutionResult.status).isEqualTo(Success);
        List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);
        assertThat(body.getFirst().get("payload")).isEqualTo("my-test-value");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        /security/truststore.jks,truststore
        /security/truststore_empty_pass.jks,''
        /security/truststore_empty_pass.jks,
        """)
    void consumer_from_target_with_truststore_should_reject_ssl_connection_with_broker_without_ssl_configured(String truststorePath, String truststorePass) throws URISyntaxException {
        // Given
        String truststore_jks = Paths.get(requireNonNull(HttpsServerStartActionTest.class.getResource(truststorePath)).toURI()).toAbsolutePath().toString();
        targetBuilder.withProperty("trustStore", truststore_jks)
            .withProperty("security.protocol", "SSL");
        ofNullable(truststorePass).ifPresent(tp ->
            targetBuilder.withProperty("trustStorePassword", truststorePass)
        );

        Target target = targetBuilder.build();

        Map<String, String> props = new HashMap<>();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP);

        // When
        Action consumeAction = getKafkaBasicConsumeAction(target, props, false);
        ActionExecutionResult actionExecutionResult = consumeAction.execute();

        // Then
        assertThat(actionExecutionResult.status).isEqualTo(Failure);
        assertThat(logger.errors).hasSize(1).first(STRING).startsWith("Unable to get the expected number of messages");
    }

    @Test
    void reset_offset_to_the_beginning() {
        // Given
        producer.send(new ProducerRecord<>(uniqueTopic, 123, "1"));

        Map<String, String> props = new HashMap<>();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name().toLowerCase());

        // When
        Action consumeAction = getKafkaBasicConsumeAction(targetBuilder.build(), props, false);
        ActionExecutionResult actionExecutionResult = consumeAction.execute();
        assertThat(actionExecutionResult.status).isEqualTo(Success);
        List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);
        assertThat(body.getFirst().get("payload")).isEqualTo("1");

        // Second time
        consumeAction = getKafkaBasicConsumeAction(targetBuilder.build(), props, false);
        actionExecutionResult = consumeAction.execute();

        assertThat(actionExecutionResult.status).isEqualTo(Failure);

        // Third time with reset
        Action consumeActionWithReset = getKafkaBasicConsumeAction(targetBuilder.build(), props, true);
        actionExecutionResult = consumeActionWithReset.execute();

        assertThat(actionExecutionResult.status).isEqualTo(Success);
        body = assertActionOutputsSize(actionExecutionResult, 1);
        assertThat(body.getFirst().get("payload")).isEqualTo("1");

        // Third time without reset
        consumeAction = getKafkaBasicConsumeAction(targetBuilder.build(), props, false);
        actionExecutionResult = consumeAction.execute();

        assertThat(actionExecutionResult.status).isEqualTo(Failure);
    }

    @Nested
    @DisplayName("Publish then consume binary message")
    class PublishThenConsume {
        @Test
        @DisplayName("simple")
        public void publish_and_consume_message_as_byte_array() {
            // Given
            TestTarget kafkaTarget = targetBuilder
                .withProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName())
                .withProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName())
                .withProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name().toLowerCase())
                .build();

            byte[] payload = "Hello la France !!".getBytes();
            var publishAction = new KafkaBasicPublishAction(
                kafkaTarget, uniqueTopic, null, payload, null, null, logger
            );
            var publishResult = publishAction.execute();
            assertThat(publishResult.status).isEqualTo(Success);

            // When
            var consumeAction = new KafkaBasicConsumeAction(
                kafkaTarget, uniqueTopic, GROUP, emptyMap(), 1, null, null,
                APPLICATION_OCTET_STREAM.getMimeType(), "3 s", null, false, logger
            );
            var actionExecutionResult = consumeAction.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Success);
            List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);
            assertThat(body.getFirst().get("payload")).isInstanceOf(byte[].class);
            assertThat(actionExecutionResult.outputs.get("payloads")).asList().first()
                .isInstanceOf(byte[].class)
                .isEqualTo(payload);
        }

        @Test
        @DisplayName("header selector")
        public void publish_and_consume_message_as_byte_array_with_header_selector() {
            // Given
            TestTarget kafkaTarget = targetBuilder
                .withProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName())
                .withProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName())
                .withProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name().toLowerCase())
                .build();

            byte[] payload = "Hello from France !!".getBytes();
            String headerValueToSelect = "uniqueHeaderValue";
            var headers = Map.of("header", headerValueToSelect);
            var publishAction = new KafkaBasicPublishAction(
                kafkaTarget, uniqueTopic, headers, payload, null, null, logger
            );
            var publishResult = publishAction.execute();
            assertThat(publishResult.status).isEqualTo(Success);

            // When
            var consumeAction = new KafkaBasicConsumeAction(
                kafkaTarget, uniqueTopic, GROUP, emptyMap(), 1, null, "$..[?($.header=='" + headerValueToSelect + "')]",
                APPLICATION_OCTET_STREAM.getMimeType(), "3 s", null, false, logger
            );
            var actionExecutionResult = consumeAction.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Success);
            List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);
            assertThat(body.getFirst().get("payload")).isInstanceOf(byte[].class);
            assertThat(actionExecutionResult.outputs.get("payloads")).asList().first()
                .isInstanceOf(byte[].class)
                .isEqualTo(payload);
        }

        @ParameterizedTest
        @ValueSource(strings = {APPLICATION_OCTET_STREAM_VALUE, APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
        @DisplayName("ignore body selector")
        public void publish_and_consume_message_as_byte_array_with_ignored_body_selector(String mimeType) {
            // Given
            TestTarget kafkaTarget = targetBuilder
                .withProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName())
                .withProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName())
                .withProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name().toLowerCase())
                .build();

            byte[] payload = "Hello from France !!".getBytes();
            var publishAction = new KafkaBasicPublishAction(
                kafkaTarget, uniqueTopic, null, payload, null, null, logger
            );
            var publishResult = publishAction.execute();
            assertThat(publishResult.status).isEqualTo(Success);

            // When
            var consumeAction = new KafkaBasicConsumeAction(
                kafkaTarget, uniqueTopic, GROUP, emptyMap(), 1, "Hello", null,
                mimeType, "3 s", null, false, logger
            );
            var actionExecutionResult = consumeAction.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Success);
            List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);
            assertThat(body.getFirst().get("payload")).isInstanceOf(byte[].class);
            assertThat(actionExecutionResult.outputs.get("payloads")).asList().first()
                .isInstanceOf(byte[].class)
                .isEqualTo(payload);
        }
    }

    private KafkaBasicConsumeAction getKafkaBasicConsumeAction(Target target, Map<String, String> props, boolean resetOffset) {
        return new KafkaBasicConsumeAction(target, uniqueTopic, GROUP, props, 1, null, null, TEXT_PLAIN_VALUE, "3 s", null, resetOffset, logger);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> assertActionOutputsSize(ActionExecutionResult actionExecutionResult, int size) {
        assertThat(actionExecutionResult.outputs).hasSize(4);

        final List<Map<String, Object>> body = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_BODY);
        final List<Map<String, Object>> payloads = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS);
        final List<Map<String, Object>> headers = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_HEADERS);
        final List<Map<String, Object>> keys = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_KEYS);
        assertThat(body).hasSize(size);
        assertThat(payloads).hasSize(size);
        assertThat(headers).hasSize(size);

        Map<String, Object> bodyTmp;
        for (int i = 0; i < body.size(); i++) {
            bodyTmp = body.get(i);
            assertThat(bodyTmp.get(OUTPUT_BODY_PAYLOAD_KEY)).isEqualTo(payloads.get(i));
            assertThat(bodyTmp.get(OUTPUT_BODY_HEADERS_KEY)).isEqualTo(headers.get(i));
            assertThat(bodyTmp.get(OUTPUT_BODY_KEY_KEY)).isEqualTo(keys.get(i));
        }

        return body;
    }

    private static Producer<Integer, String> createProducer(String brokerPath) {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerPath);
        return new DefaultKafkaProducerFactory<>(producerProps, new IntegerSerializer(), new StringSerializer()).createProducer();
    }
}
