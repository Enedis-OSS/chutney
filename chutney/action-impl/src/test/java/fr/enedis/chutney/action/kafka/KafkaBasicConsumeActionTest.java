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
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Target;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeType;

@SuppressWarnings({"unchecked", "rawtypes"})
public class KafkaBasicConsumeActionTest {

    private static final String TOPIC = "topic";
    private static final String GROUP = "mygroup";
    private static final long TIMESTAMP = 42L;
    private static final TimestampType TIMESTAMP_TYPE = TimestampType.CREATE_TIME;
    private static final long FIRST_OFFSET = 0L;
    private static final int PARTITION = 0;

    private static final Target TARGET_STUB = TestTarget.TestTargetBuilder.builder()
        .withTargetId("kafka")
        .withUrl("tcp://127.0.0.1:5555")
        .build();

    private TestLogger logger;


    @BeforeEach
    public void before() {
        logger = new TestLogger();
    }

    @Test
    void set_inputs_default_values() {
        KafkaBasicConsumeAction defaultAction = new KafkaBasicConsumeAction(null, null, null, null, null, null, null, null, null, null, null, null);
        assertThat(defaultAction)
            .hasFieldOrPropertyWithValue("target", null)
            .hasFieldOrPropertyWithValue("topic", null)
            .hasFieldOrPropertyWithValue("group", null)
            .hasFieldOrPropertyWithValue("properties", emptyMap())
            .hasFieldOrPropertyWithValue("nbMessages", 1)
            .hasFieldOrPropertyWithValue("selector", null)
            .hasFieldOrPropertyWithValue("headerSelector", null)
            .hasFieldOrPropertyWithValue("contentType", MimeType.valueOf("application/json"))
            .hasFieldOrPropertyWithValue("timeout", "60 sec")
            .hasFieldOrPropertyWithValue("ackMode", "BATCH")
            .hasFieldOrPropertyWithValue("resetOffset", false)
            .hasFieldOrPropertyWithValue("logger", null)
        ;
    }

    @Nested
    @DisplayName("Validate inputs")
    class ValidateInputs {
        @Test
        void validate_all_mandatory_inputs() {
            KafkaBasicConsumeAction defaultAction = new KafkaBasicConsumeAction(null, null, null, null, null, null, null, null, null, null, null, null);
            List<String> errors = defaultAction.validateInputs();

            assertThat(errors.size()).isEqualTo(8);
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(errors.getFirst()).isEqualTo("No topic provided (String)");
            softly.assertThat(errors.get(1)).isEqualTo("topic should not be blank");

            softly.assertThat(errors.get(2)).isEqualTo("No group provided (String)");
            softly.assertThat(errors.get(3)).isEqualTo("group should not be blank");

            softly.assertThat(errors.get(4)).isEqualTo("No target provided");
            softly.assertThat(errors.get(5)).isEqualTo("[Target name is blank] not applied because of exception java.lang.NullPointerException(null)");
            softly.assertThat(errors.get(6)).isEqualTo("[Target url is not valid: null target] not applied because of exception java.lang.NullPointerException(null)");
            softly.assertThat(errors.get(7)).isEqualTo("[Target url has an undefined host: null target] not applied because of exception java.lang.NullPointerException(null)");

            softly.assertAll();
        }

        @Test
        void validate_timeout_input() {
            String badTimeout = "twenty seconds";
            KafkaBasicConsumeAction defaultAction = new KafkaBasicConsumeAction(TARGET_STUB, "topic", "group", null, null, null, null, null, badTimeout, null, null, null);

            List<String> errors = defaultAction.validateInputs();

            assertThat(errors.size()).isEqualTo(1);
            assertThat(errors.getFirst()).startsWith("[timeout is not parsable]");
        }

        @Test
        void validate_ackMode_input() {
            String badTackMode = "UNKNOWN_ACKMODE";
            KafkaBasicConsumeAction defaultAction = new KafkaBasicConsumeAction(TARGET_STUB, "topic", "group", null, null, null, null, null, null, badTackMode, null, null);

            List<String> errors = defaultAction.validateInputs();

            assertThat(errors.size()).isEqualTo(1);
            assertThat(errors.getFirst()).startsWith("ackMode is not a valid value");
        }
    }

    @Nested
    @DisplayName("Check for no messages")
    class NoMessages {

        @Test
        void not_find_any_message() {
            // Given
            Action sut = givenKafkaConsumeAction(0, null, null, TEXT_PLAIN_VALUE, "3 sec");

            // When
            ActionExecutionResult actionExecutionResult = sut.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Success);
            assertActionOutputsSize(actionExecutionResult, 0);
            assertThat(logger.errors).isEmpty();
        }

        @Test
        void not_find_any_message_failed() {
            // Given
            Action sut = givenKafkaConsumeAction(0, null, null, TEXT_PLAIN_VALUE, "3 sec");
            givenActionReceiveMessages(sut,
                buildRecord(FIRST_OFFSET, "KEY", "test message")
            );

            // When
            ActionExecutionResult actionExecutionResult = sut.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Failure);
            assertThat(logger.errors).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Consume messages")
    class ConsumeMessage {

        @Nested
        @DisplayName("Simple text, json or xml")
        class Simple {

            @Test
            void consume_simple_text_message() {
                // Given
                Action sut = givenKafkaConsumeAction(null, TEXT_PLAIN_VALUE, null);
                givenActionReceiveMessages(sut,
                    buildRecord(FIRST_OFFSET, "KEY", "test message")
                );

                // When
                ActionExecutionResult actionExecutionResult = sut.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);
                List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);

                final Map<String, Object> message = body.getFirst();
                final String payload1 = (String) message.get(OUTPUT_BODY_PAYLOAD_KEY);
                assertThat(payload1).isEqualTo("test message");
                final Map<String, Object> headers = (Map<String, Object>) message.get(OUTPUT_BODY_HEADERS_KEY);
                assertThat(headers.get("X-Custom-HeaderKey")).isEqualTo("X-Custom-HeaderValue");
                assertThat(headers).containsAllEntriesOf(ImmutableMap.of("X-Custom-HeaderKey", "X-Custom-HeaderValue", "header1", "value1"));

                assertThat(logger.errors).isEmpty();
            }

            @Test
            void consume_simple_text_message_with_multiple_values_for_the_same_header() {
                // Given
                ImmutableList<Header> headers = ImmutableList.of(
                    new RecordHeader("key-with-multiple-values", "value 1".getBytes()),
                    new RecordHeader("key-with-multiple-values", "value 2".getBytes())
                );
                Action sut = givenKafkaConsumeAction(null, TEXT_PLAIN_VALUE, null);
                givenActionReceiveMessages(sut,
                    buildRecord(FIRST_OFFSET, "KEY", "test message", headers)
                );

                // When
                ActionExecutionResult actionExecutionResult = sut.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);
                var result_headers = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_BODY_HEADERS_KEY);
                assertThat(result_headers).hasSize(1);
                assertThat(result_headers.getFirst())
                    .containsExactly(
                        Map.entry("key-with-multiple-values", asList("value 1", "value 2"))
                    );
            }

            @ParameterizedTest
            @ValueSource(strings = APPLICATION_JSON_VALUE)
            @NullSource
            void consume_json_message_as_map(String mimeType) {
                // Given
                Action action = givenKafkaConsumeAction(null, mimeType, null);
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, 1, "{\"value\": \"test message\", \"id\": \"1111\" }")
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);

                final Map<String, Object> payload = ((List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS)).getFirst();
                assertThat(payload.get("value")).isEqualTo("test message");
                assertThat(payload.get("id")).isEqualTo("1111");
            }

            @Test
            void consume_xml_message_as_string() {
                // Given
                Action action = givenKafkaConsumeAction(null, APPLICATION_XML_VALUE, null);
                String xmlPayload = "<root><first>first content</first><second attr=\"second attr\">second content</second></root>";
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, 1, xmlPayload)
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);

                final String payload = ((List<String>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS)).getFirst();
                assertThat(payload).isEqualTo(xmlPayload);
            }
        }

        @Nested
        @DisplayName("By header JSON Path selection or payload selection given mime type")
        class Selection {

            @Test
            void select_json_message_whose_payload_or_headers_match_given_payload_jsonpath_selector() {
                // Given
                Action action = givenKafkaConsumeAction("$..[?($.headers.header1=='value1' && $.payload.id==\"1122\")]", APPLICATION_JSON_VALUE, null);
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, "KEY1", "{\"value\": \"test message1\", \"id\": \"1111\" }"),
                    buildRecord(FIRST_OFFSET + 1, "KEY2", "{\"value\": \"test message2\", \"id\": \"1122\" }"),
                    buildRecord(FIRST_OFFSET + 2, "KEY3", "{\"value\": \"test message3\", \"id\": \"1133\" }")
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);

                List<Map<String, Object>> body = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_BODY);
                assertThat(body).hasSize(1);
                final Map<String, Object> payload = ((List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS)).getFirst();
                assertThat(payload.get("id")).isEqualTo("1122");
            }

            @Test
            void select_xml_message_whose_payload_match_given_payload_xpath_selector() {
                // Given
                Action action = givenKafkaConsumeAction("/root/second[@attr='1122']", APPLICATION_XML_VALUE, null);
                String payloadToSelect = "<root><first>first content</first><second attr=\"1122\">second content</second></root>";
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, "KEY1", "<root><first>first content</first><second attr=\"1111\">second content</second></root>"),
                    buildRecord(FIRST_OFFSET + 1, "KEY2", payloadToSelect),
                    buildRecord(FIRST_OFFSET + 2, "KEY3", "<root><first>first content</first><second attr=\"1133\">second content</second></root>")
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);

                List<Map<String, Object>> body = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_BODY);
                assertThat(body).hasSize(1);
                final String payload = ((List<String>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS)).getFirst();
                assertThat(payload).isEqualTo(payloadToSelect);
            }

            @Test
            void select_text_message_whose_payload_contains_given_payload_selector() {
                // Given
                Action action = givenKafkaConsumeAction("selector", TEXT_PLAIN_VALUE, null);
                String payloadToSelect = "second text selector message";
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, 1L, "first text message"),
                    buildRecord(FIRST_OFFSET + 1, 2L, payloadToSelect),
                    buildRecord(FIRST_OFFSET + 2, 3L, "third text message")
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);

                List<Map<String, Object>> body = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_BODY);
                assertThat(body).hasSize(1);
                final String payload = ((List<String>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS)).getFirst();
                assertThat(payload).isEqualTo(payloadToSelect);
            }

            @Test
            void select_message_whose_headers_match_given_payload_jsonpath_selector() {
                // Given
                ImmutableList<Header> headers = ImmutableList.of(
                    new RecordHeader("X-Custom-HeaderKey", "X-Custom-HeaderValue".getBytes()),
                    new RecordHeader("header", "123".getBytes()),
                    new RecordHeader("key-with-multiple-values", "value 1".getBytes()),
                    new RecordHeader("key-with-multiple-values", "value 2".getBytes())
                );
                Action action = givenKafkaConsumeAction(3, null, "$..[?($.header=='123' && $.key-with-multiple-values contains 'value 1')]", null, null);
                String textMessageToSelect = "first text message";
                String xmlMessageToSelect = "<root>first xml message</root>";
                String jsonMessageToSelect = "first json message";
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, "KEY1", textMessageToSelect, headers),
                    buildRecord(FIRST_OFFSET + 1, "KEY2", "second text message"),
                    buildRecord(FIRST_OFFSET + 2, "KEY3", xmlMessageToSelect, headers),
                    buildRecord(FIRST_OFFSET + 3, "KEY4", "<root>second xml message</root>"),
                    buildRecord(FIRST_OFFSET + 4, "KEY5", "{\"value\": \"" + jsonMessageToSelect + "\", \"id\": \"1\" }", headers),
                    buildRecord(FIRST_OFFSET + 5, "KEY6", "{\"value\": \"second json message\", \"id\": \"1\" }")
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);
                List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 3);

                final String payload = (String) body.getFirst().get(OUTPUT_BODY_PAYLOAD_KEY);
                assertThat(payload).isEqualTo(textMessageToSelect);
                final String xmlPayload = (String) body.get(1).get(OUTPUT_BODY_PAYLOAD_KEY);
                assertThat(xmlPayload).isEqualTo(xmlMessageToSelect);
                final Map<String, String> jsonPayload = (Map<String, String>) body.get(2).get(OUTPUT_BODY_PAYLOAD_KEY);
                assertThat(jsonPayload.get("value")).isEqualTo(jsonMessageToSelect);
            }

            @Test
            void consume_message_with_duplicated_header_pair_key_value() {
                // Given
                ImmutableList<Header> headers = ImmutableList.of(
                    new RecordHeader("header", "123".getBytes()),
                    new RecordHeader("header", "123".getBytes())
                );
                Action action = givenKafkaConsumeAction(1, null, "$..[?($.header=='123')]", null, null);
                String textMessageToSelect = "first text message";
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, "KEY1", textMessageToSelect, headers),
                    buildRecord(FIRST_OFFSET + 1, "KEY2", "second text message")
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);
                List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);

                final String payload = (String) body.getFirst().get(OUTPUT_BODY_PAYLOAD_KEY);
                assertThat(payload).isEqualTo(textMessageToSelect);
            }

            @Test
            void not_filter_by_given_mime_type_when_no_selector() {
                // Given
                ImmutableList<Header> headers = ImmutableList.of(
                    new RecordHeader("X-Custom-HeaderKey", "X-Custom-HeaderValue".getBytes()),
                    new RecordHeader("Content-Type", APPLICATION_JSON_VALUE.getBytes())
                );

                Action action = givenKafkaConsumeAction(2, null, null, APPLICATION_XML_VALUE, null);
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, "KEY1", "{\"value\": \"test message\", \"id\": \"1\" }", headers),
                    buildRecord(FIRST_OFFSET + 1, "KEY2", "<root>second test message</root>", ImmutableList.of(
                        new RecordHeader("X-Custom-HeaderKey", "X-Custom-HeaderValue".getBytes())
                    ))
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);
                List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 2);

                assertThat(body.getFirst().get(OUTPUT_BODY_PAYLOAD_KEY)).isEqualTo(Map.of("id", "1", "value", "test message"));
                assertThat(body.get(1).get(OUTPUT_BODY_PAYLOAD_KEY)).isEqualTo("<root>second test message</root>");
            }

            @Test
            void filter_by_given_mime_type_when_selector_is_defined() {
                // Given
                ImmutableList<Header> headers = ImmutableList.of(
                    new RecordHeader("X-Custom-HeaderKey", "X-Custom-HeaderValue".getBytes()),
                    new RecordHeader("Content-Type", APPLICATION_JSON_VALUE.getBytes())
                );

                Action action = givenKafkaConsumeAction("//root", APPLICATION_XML_VALUE, null);
                givenActionReceiveMessages(action,
                    buildRecord(FIRST_OFFSET, 1, "{\"value\": \"test message\", \"id\": \"1\" }", headers),
                    buildRecord(FIRST_OFFSET + 1, 2, "<root>second test message</root>", ImmutableList.of(
                        new RecordHeader("X-Custom-HeaderKey", "X-Custom-HeaderValue".getBytes())
                    ))
                );

                // When
                ActionExecutionResult actionExecutionResult = action.execute();

                // Then
                assertThat(actionExecutionResult.status).isEqualTo(Success);
                List<Map<String, Object>> body = assertActionOutputsSize(actionExecutionResult, 1);

                assertThat(body.getFirst().get(OUTPUT_BODY_PAYLOAD_KEY)).isEqualTo("<root>second test message</root>");
            }
        }

        @Test
        @DisplayName("By number")
        void return_exactly_nb_message_asked() {
            // Given
            Action sut = givenKafkaConsumeAction(1, null, null, TEXT_PLAIN_VALUE, "3 sec");
            givenActionReceiveMessages(sut,
                buildRecord(FIRST_OFFSET, "KEY", "test message"),
                buildRecord(FIRST_OFFSET + 1, "KEY2", "test message2")
            );

            // When
            ActionExecutionResult actionExecutionResult = sut.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Success);
            assertActionOutputsSize(actionExecutionResult, 1);
        }

        @Test
        @DisplayName("With timeout")
        void respect_given_timeout() {
            // Given
            Action action = givenKafkaConsumeAction(null, null, "3 sec");
            overrideActionMessageListenerContainer(action);

            // When
            ActionExecutionResult actionExecutionResult = action.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Failure);
            assertThat(logger.errors).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"bad content type", APPLICATION_JSON_VALUE, "\"" + APPLICATION_JSON_VALUE + "\""})
        void consume_as_json_with_bad_content_type_in_received_message(String contentType) {
            ImmutableList<Header> headers = ImmutableList.of(
                new RecordHeader("Content-type", contentType.getBytes())
            );
            // Given
            Action action = givenKafkaConsumeAction(null, null, null);
            givenActionReceiveMessages(action,
                buildRecord(FIRST_OFFSET, 1, "{\"value\": \"test message\", \"id\": \"1111\" }", headers)
            );

            // When
            ActionExecutionResult actionExecutionResult = action.execute();

            // Then
            assertThat(actionExecutionResult.status).isEqualTo(Success);

            final Map<String, Object> payload = ((List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS)).getFirst();
            assertThat(payload.get("value")).isEqualTo("test message");
            assertThat(payload.get("id")).isEqualTo("1111");
        }
    }


    // todo mock kafka consumer
    private MessageListener overrideActionMessageListenerContainer(Action action) {
        ConsumerFactory cf = mock(ConsumerFactory.class, RETURNS_DEEP_STUBS);
        Consumer consumer = mock(Consumer.class);
        given(cf.createConsumer(any(), any(), any(), any())).willReturn(consumer);
        when(cf.getConfigurationProperties().get(eq(ConsumerConfig.GROUP_ID_CONFIG))).thenReturn(GROUP);

        KafkaConsumerFactory kafkaConsumerFactory = mock(KafkaConsumerFactory.class);
        ReflectionTestUtils.setField(action, "kafkaConsumerFactory", kafkaConsumerFactory);

        ContainerProperties containerProperties = new ContainerProperties(TOPIC);
        containerProperties.setGroupId(GROUP);
        containerProperties.setMessageListener(requireNonNull(ReflectionTestUtils.invokeMethod(action, "createMessageListener")));
        var messageListenerContainer = new ConcurrentMessageListenerContainer<>(cf, containerProperties);

        when(kafkaConsumerFactory.create(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(messageListenerContainer);

        return (MessageListener) messageListenerContainer.getContainerProperties().getMessageListener();
    }

    private ConsumerRecord buildRecord(long offset, Object key, Object payload) {
        List<Header> headersList = ImmutableList.of(new RecordHeader("X-Custom-HeaderKey", "X-Custom-HeaderValue".getBytes()), new RecordHeader("header1", "value1".getBytes()));
        return new ConsumerRecord(TOPIC, PARTITION, offset, TIMESTAMP, TIMESTAMP_TYPE, 0, 0, key, payload, new RecordHeaders(headersList), empty());
    }

    private ConsumerRecord buildRecord(long offset, Object key, Object payload, List<Header> headersList) {
        return new ConsumerRecord(TOPIC, PARTITION, offset, TIMESTAMP, TIMESTAMP_TYPE, 0, 0, key, payload, new RecordHeaders(headersList), empty());
    }

    private KafkaBasicConsumeAction givenKafkaConsumeAction(String selector, String mimeType, String timeout) {
        return givenKafkaConsumeAction(1, selector, null, mimeType, timeout);
    }

    private KafkaBasicConsumeAction givenKafkaConsumeAction(int expectedMessageNb, String selector, String headerSelector, String mimeType, String timeout) {
        return new KafkaBasicConsumeAction(TARGET_STUB, TOPIC, GROUP, emptyMap(), expectedMessageNb, selector, headerSelector, mimeType, timeout, null, null, logger);
    }

    private void givenActionReceiveMessages(Action action, ConsumerRecord... messages) {
        MessageListener listener = overrideActionMessageListenerContainer(action);
        stream(messages).forEach(listener::onMessage);
    }

    private List<Map<String, Object>> assertActionOutputsSize(ActionExecutionResult actionExecutionResult, int size) {
        assertThat(actionExecutionResult.outputs).hasSize(4);

        final var body = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_BODY);
        final var payloads = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_PAYLOADS);
        final var headers = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_HEADERS);
        final var keys = (List<Map<String, Object>>) actionExecutionResult.outputs.get(OUTPUT_KEYS);
        assertThat(body).hasSize(size);
        assertThat(payloads).hasSize(size);
        assertThat(headers).hasSize(size);
        assertThat(keys).hasSize(size);

        Map<String, Object> bodyTmp;
        for (int i = 0; i < body.size(); i++) {
            bodyTmp = body.get(i);
            assertThat(bodyTmp.get(OUTPUT_BODY_PAYLOAD_KEY)).isEqualTo(payloads.get(i));
            assertThat(bodyTmp.get(OUTPUT_BODY_HEADERS_KEY)).isEqualTo(headers.get(i));
            assertThat(bodyTmp.get(OUTPUT_BODY_KEY_KEY)).isEqualTo(keys.get(i));
        }

        return body;
    }
}
