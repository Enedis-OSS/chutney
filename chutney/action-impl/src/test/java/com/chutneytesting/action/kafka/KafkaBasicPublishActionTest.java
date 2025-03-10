/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.kafka;

import com.chutneytesting.action.TestLogger;
import com.chutneytesting.action.TestTarget;
import com.chutneytesting.action.http.HttpsServerStartActionTest;
import com.chutneytesting.action.spi.Action;
import com.chutneytesting.action.spi.ActionExecutionResult;
import static com.chutneytesting.action.spi.ActionExecutionResult.Status.Failure;
import static com.chutneytesting.action.spi.ActionExecutionResult.Status.Success;
import com.chutneytesting.action.spi.injectable.Target;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import static java.util.Collections.emptyMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
public class KafkaBasicPublishActionTest {

    private static final String TOPIC = "topic";
    private static final String PAYLOAD = "payload";
    private static final String GROUP = "mygroup";
    private static final EmbeddedKafkaBroker embeddedKafkaBroker = new EmbeddedKafkaZKBroker(1, true, TOPIC);

    private TestLogger logger;

    @BeforeEach
    public void before() {
        logger = new TestLogger();
    }

    @AfterAll
    public static void afterAll() {
        embeddedKafkaBroker.destroy();
    }

    private Target getKafkaTarget() {
        return TestTarget.TestTargetBuilder.builder()
            .withTargetId("kafka")
            .withUrl("127.0.0.1:5555")
            .build();
    }

    @Test
    void should_set_inputs_default_values() {
        KafkaBasicPublishAction defaultAction = new KafkaBasicPublishAction(null, null, null, null, null, null, null);
        assertThat(defaultAction)
            .hasFieldOrPropertyWithValue("target", null)
            .hasFieldOrPropertyWithValue("topic", null)
            .hasFieldOrPropertyWithValue("headers", emptyMap())
            .hasFieldOrPropertyWithValue("payload", null)
            .hasFieldOrPropertyWithValue("producerKafkaConfig", emptyMap())
            .hasFieldOrPropertyWithValue("key", null)
            .hasFieldOrPropertyWithValue("logger", null)
        ;
    }

    @Test
    void should_validate_all_mandatory_inputs() {
        KafkaBasicPublishAction defaultAction = new KafkaBasicPublishAction(null, null, null, null, null, null, null);
        List<String> errors = defaultAction.validateInputs();

        assertThat(errors.size()).isEqualTo(7);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(errors.getFirst()).isEqualTo("No topic provided (String)");
        softly.assertThat(errors.get(1)).isEqualTo("topic should not be blank");

        softly.assertThat(errors.get(2)).isEqualTo("No payload provided");

        softly.assertThat(errors.get(3)).isEqualTo("No target provided");
        softly.assertThat(errors.get(4)).isEqualTo("[Target name is blank] not applied because of exception java.lang.NullPointerException(null)");
        softly.assertThat(errors.get(5)).isEqualTo("[Target url is not valid: null target] not applied because of exception java.lang.NullPointerException(null)");
        softly.assertThat(errors.get(6)).isEqualTo("[Target url has an undefined host: null target] not applied because of exception java.lang.NullPointerException(null)");

        softly.assertAll();
    }

    @Test
    public void basic_publish_action_should_success() throws Exception {
        //given
        TestLogger logger = new TestLogger();
        Action action = new KafkaBasicPublishAction(getKafkaTarget(), TOPIC, null, PAYLOAD, null, null, logger);
        //mocks
        ChutneyKafkaProducerFactory producerFactoryMock = mock(ChutneyKafkaProducerFactory.class);
        KafkaTemplate kafkaTemplateMock = mock(KafkaTemplate.class);
        when(producerFactoryMock.create(any(), any())).thenReturn(kafkaTemplateMock);

        CompletableFuture<SendResult<String, String>> listenableFutureMock = mock(CompletableFuture.class);
        when(listenableFutureMock.get(anyLong(), any(TimeUnit.class))).thenReturn(null);
        when(kafkaTemplateMock.send(ArgumentMatchers.<ProducerRecord<String, String>>any())).thenReturn(listenableFutureMock);

        ReflectionTestUtils.setField(action, "producerFactory", producerFactoryMock);

        //when
        ActionExecutionResult actionExecutionResult = action.execute();

        //Then
        assertThat(actionExecutionResult.status).isEqualTo(Success);
        assertThat(logger.errors).isEmpty();
        verify(listenableFutureMock).get(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void basic_publish_action_should_failed_when_timeout() throws Exception {
        //given
        TestLogger logger = new TestLogger();
        Action action = new KafkaBasicPublishAction(getKafkaTarget(), TOPIC, null, PAYLOAD, null, null, logger);
        //mocks
        ChutneyKafkaProducerFactory producerFactoryMock = mock(ChutneyKafkaProducerFactory.class);
        KafkaTemplate kafkaTemplateMock = mock(KafkaTemplate.class);
        when(producerFactoryMock.create(any(), any())).thenReturn(kafkaTemplateMock);

        CompletableFuture<SendResult<String, String>> listenableFutureMock = mock(CompletableFuture.class);
        when(listenableFutureMock.get(anyLong(), any(TimeUnit.class))).thenThrow(TimeoutException.class);
        when(kafkaTemplateMock.send(ArgumentMatchers.<ProducerRecord<String, String>>any())).thenReturn(listenableFutureMock);

        ReflectionTestUtils.setField(action, "producerFactory", producerFactoryMock);

        //when
        ActionExecutionResult actionExecutionResult = action.execute();

        //Then
        assertThat(actionExecutionResult.status).isEqualTo(Failure);
        assertThat(logger.errors).isNotEmpty();
    }

    @Test
    public void should_produce_message_to_broker_without_truststore() {
        embeddedKafkaBroker.afterPropertiesSet();
        try (Consumer<Integer, String> consumer = configureConsumer()) {

            Target target = TestTarget.TestTargetBuilder.builder()
                .withTargetId("kafka")
                .withUrl("tcp://" + embeddedKafkaBroker.getBrokersAsString())
                .build();

            Map<String, String> props = new HashMap<>();
            props.put("group.id", GROUP);

            Action sut = new KafkaBasicPublishAction(target, TOPIC, Map.of(), "my-test-value", props, null, logger);

            ActionExecutionResult actionExecutionResult = sut.execute();

            assertThat(actionExecutionResult.status).isEqualTo(Success);

            ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, TOPIC);
            assertThat(singleRecord.value()).isEqualTo("my-test-value");
        }
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        /security/truststore.jks,truststore
        /security/truststore_empty_pass.jks,''
        /security/truststore_empty_pass.jks,
        """)
    public void producer_from_target_with_truststore_should_reject_ssl_connection_with_broker_without_ssl_configured(String truststorePath, String truststorePass) throws URISyntaxException {
        embeddedKafkaBroker.afterPropertiesSet();

        String truststore_jks = Paths.get(requireNonNull(HttpsServerStartActionTest.class.getResource(truststorePath)).toURI()).toAbsolutePath().toString();
        var targetBuilder = TestTarget.TestTargetBuilder.builder()
            .withTargetId("kafka")
            .withUrl("tcp://" + embeddedKafkaBroker.getBrokersAsString())
            .withProperty("trustStore", truststore_jks)
            .withProperty("security.protocol", "SSL");

        ofNullable(truststorePass).ifPresent(tp ->
            targetBuilder.withProperty("trustStorePassword", truststorePass)
        );

        Target target = targetBuilder.build();

        Map<String, String> props = new HashMap<>();
        props.put("group.id", GROUP);
        props.put("max.block.ms", "500");

        Action sut = new KafkaBasicPublishAction(target, TOPIC, Map.of(), "my-test-value", props, null, logger);

        ActionExecutionResult actionExecutionResult = sut.execute();

        assertThat(actionExecutionResult.status).isEqualTo(Failure);
        assertThat(logger.errors).hasSize(1).first(STRING).endsWith("Send failed");
    }

    @Test
    public void should_produce_message_to_broker_with_explicit_key() {
        embeddedKafkaBroker.afterPropertiesSet();
        try (Consumer<Integer, String> consumer = configureConsumer()) {

            Target target = TestTarget.TestTargetBuilder.builder()
                .withTargetId("kafka")
                .withUrl("tcp://" + embeddedKafkaBroker.getBrokersAsString())
                .build();

            var props = Map.of("group.id", GROUP);

            Action sut = new KafkaBasicPublishAction(target, TOPIC, Map.of(), "my-test-value", props, "my-key", logger);

            ActionExecutionResult actionExecutionResult = sut.execute();

            assertThat(actionExecutionResult.status).isEqualTo(Success);

            ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, TOPIC);
            assertThat(singleRecord.value()).isEqualTo("my-test-value");
            assertThat(String.valueOf(singleRecord.key())).isEqualTo("my-key");
        }
    }

    private Consumer<Integer, String> configureConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put("key.deserializer", StringDeserializer.class.getName());

        Consumer<Integer, String> consumer = new DefaultKafkaConsumerFactory<Integer, String>(consumerProps)
            .createConsumer();
        consumer.subscribe(Collections.singleton(TOPIC));
        return consumer;
    }
}
