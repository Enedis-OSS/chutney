/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.kafka;

import static java.util.Collections.emptyMap;
import static java.util.Collections.shuffle;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import com.chutneytesting.action.TestTarget;
import com.chutneytesting.action.spi.injectable.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;

class KafkaConsumerFactoryTest {

    private final TestTarget.TestTargetBuilder targetWithoutProperties = TestTarget.TestTargetBuilder.builder()
        .withTargetId("kafka")
        .withUrl("tcp://127.0.0.1:5555");

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("set kafka client bootstrap.servers configuration")
    class BootstrapServersConfig {

        @Test
        @Order(1)
        @DisplayName("from configuration")
        void use_configuration_for_bootstrap_servers_kafka_client_configuration() {
            Target target = targetWithoutProperties
                .withProperty(BOOTSTRAP_SERVERS_CONFIG, "target.host:1236")
                .build();

            Map<String, String> config = Map.of(BOOTSTRAP_SERVERS_CONFIG, "conf.host:4567");

            var sut = new KafkaConsumerFactory();
            var listenerContainer = getListenerContainer(sut, target, config);

            assertConsumerFactoryConfigs(listenerContainer)
                .containsEntry(BOOTSTRAP_SERVERS_CONFIG, config.get(BOOTSTRAP_SERVERS_CONFIG));
        }

        @Test
        @Order(2)
        @DisplayName("from target properties")
        void use_target_properties_for_bootstrap_servers_kafka_client_configuration() {
            Target target = targetWithoutProperties
                .withProperty(BOOTSTRAP_SERVERS_CONFIG, "target.host:1236")
                .build();

            var sut = new KafkaConsumerFactory();
            var listenerContainer = getListenerContainer(sut, target, emptyMap());

            assertConsumerFactoryConfigs(listenerContainer)
                .containsEntry(BOOTSTRAP_SERVERS_CONFIG, target.property(BOOTSTRAP_SERVERS_CONFIG).get());
        }

        @Test
        @Order(3)
        @DisplayName("from target's url authority")
        void use_target_authority_url_for_bootstrap_servers_kafka_client_configuration() {
            Target target = targetWithoutProperties.build();

            var sut = new KafkaConsumerFactory();
            var listenerContainer = getListenerContainer(sut, target, emptyMap());

            assertConsumerFactoryConfigs(listenerContainer)
                .hasEntrySatisfying(BOOTSTRAP_SERVERS_CONFIG, (v) -> assertThat(v).isEqualTo("127.0.0.1:5555"));
        }

        @Test
        @Order(4)
        @DisplayName("from whole target's url otherwise")
        void use_target_host_url_for_bootstrap_servers_kafka_client_configuration() {
            Target target = targetWithoutProperties
                .withUrl("http:/a/path")
                .build();

            var sut = new KafkaConsumerFactory();
            var listenerContainer = getListenerContainer(sut, target, emptyMap());

            assertConsumerFactoryConfigs(listenerContainer)
                .hasEntrySatisfying(BOOTSTRAP_SERVERS_CONFIG, (v) -> assertThat(v).isEqualTo(target.uri().toString()));
        }
    }

    @Test
    void default_configuration() {
        Target target = targetWithoutProperties.build();

        var sut = new KafkaConsumerFactory();
        var listenerContainer = getListenerContainer(sut, target, emptyMap());

        assertConsumerFactoryConfigs(listenerContainer)
            .containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
            .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
        ;
    }

    @Test
    void filter_and_merge_kafka_consumer_configuration_properties_from_target_and_input() {
        List<String> consumerConfigKeys = new ArrayList<>(ConsumerConfig.configNames());
        shuffle(consumerConfigKeys);
        consumerConfigKeys.remove(AUTO_COMMIT_INTERVAL_MS_CONFIG);
        String targetProperty = consumerConfigKeys.get(0);
        String propertyToOverride = consumerConfigKeys.get(1);
        String inputProperty = consumerConfigKeys.get(2);
        String notConsumerConfigTargetProperty = "not.a.kafka.consumer.config.key";
        String notConsumerConfigInputProperty = "not.another.kafka.consumer.config.key";

        Target target = TestTarget.TestTargetBuilder.builder()
            .withProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker.host")
            .withProperty(targetProperty, "a value")
            .withProperty(propertyToOverride, "a target value")
            .withProperty(notConsumerConfigTargetProperty, "to filter")
            .build();

        Map<String, String> properties = Map.of(
            inputProperty, "a VALUE",
            propertyToOverride, "a property value",
            notConsumerConfigInputProperty, "to filter"
        );

        Map<String, String> expectedConfig = Map.of(
            targetProperty, "a value",
            inputProperty, "a VALUE",
            propertyToOverride, "a property value"
        );

        var sut = new KafkaConsumerFactory();
        var listenerContainer = getListenerContainer(sut, target, properties);

        assertConsumerFactoryConfigs(listenerContainer)
            .containsAllEntriesOf(expectedConfig)
            .doesNotContainKeys(notConsumerConfigTargetProperty, notConsumerConfigInputProperty)
        ;
    }

    private static MessageListenerContainer getListenerContainer(KafkaConsumerFactory sut, Target target, Map<String, String> config) {
        return sut.create(
            target, "", "", false, "BATCH", record -> {
            }, null, config
        );
    }

    private static MapAssert<Object, Object> assertConsumerFactoryConfigs(MessageListenerContainer listenerContainer) {
        return assertThat(listenerContainer).extracting("consumerFactory")
            .isInstanceOf(ConsumerFactory.class)
            .extracting("configs", as(InstanceOfAssertFactories.MAP));
    }
}
