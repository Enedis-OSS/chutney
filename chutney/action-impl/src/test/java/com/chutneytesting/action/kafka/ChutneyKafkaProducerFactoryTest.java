/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.kafka;

import static java.util.Collections.emptyMap;
import static java.util.Collections.shuffle;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.chutneytesting.action.TestTarget;
import com.chutneytesting.action.spi.injectable.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChutneyKafkaProducerFactoryTest - Build Kafka template")
class ChutneyKafkaProducerFactoryTest {

    @Test
    void default_configuration() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withUrl("tpc://kafka.broker.host")
            .build();
        var sut = new ChutneyKafkaProducerFactory();
        var kafkaTemplate = sut.create(target, emptyMap());
        assertThat(kafkaTemplate.getProducerFactory().getConfigurationProperties())
            .containsOnly(
                entry(BOOTSTRAP_SERVERS_CONFIG, target.uri().getHost()),
                entry(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName()),
                entry(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName())
            );
    }

    @Test
    void override_boostrap_servers_from_target() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withUrl("tpc://kafka.broker.host")
            .build();
        Map<String, String> config = new HashMap<>();
        config.put(BOOTSTRAP_SERVERS_CONFIG, "first.kafka.broker.host,second.kafka.broker.host");

        var sut = new ChutneyKafkaProducerFactory();
        var kafkaTemplate = sut.create(target, config);
        assertThat(kafkaTemplate.getProducerFactory().getConfigurationProperties())
            .containsEntry(BOOTSTRAP_SERVERS_CONFIG, config.get(BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void set_or_override_truststore_from_target() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withUrl("tpc://kafka.broker.host")
            .withProperty("trustStore", "file://path/to/truststore")
            .withProperty("trustStorePassword", "TrustPass")
            .withProperty(SSL_TRUSTSTORE_LOCATION_CONFIG, "overridden value")
            .withProperty(SSL_TRUSTSTORE_PASSWORD_CONFIG, "overridden value")
            .build();

        var sut = new ChutneyKafkaProducerFactory();
        var kafkaTemplate = sut.create(target, emptyMap());
        assertThat(kafkaTemplate.getProducerFactory().getConfigurationProperties())
            .containsEntry(SSL_TRUSTSTORE_LOCATION_CONFIG, target.trustStore().get())
            .containsEntry(SSL_TRUSTSTORE_PASSWORD_CONFIG, target.trustStorePassword().get());
    }

    @Test
    void filter_and_merge_kafka_producer_configuration_properties_from_target_and_input() {
        List<String> producerConfigKeys = new ArrayList<>(ProducerConfig.configNames());
        shuffle(producerConfigKeys);
        List.of(
            TRANSACTIONAL_ID_CONFIG // Used by Spring DefaultKafkaProducerFactory as transaction id prefix
        ).forEach(producerConfigKeys::remove);
        String targetProperty = producerConfigKeys.getFirst();
        String propertyToOverride = producerConfigKeys.get(1);
        String inputProperty = producerConfigKeys.get(2);
        String notProducerConfigTargetProperty = "not.a.kafka.producer.config.key";
        String notProducerConfigInputProperty = "not.another.kafka.producer.config.key";

        Target target = TestTarget.TestTargetBuilder.builder()
            .withProperty(BOOTSTRAP_SERVERS_CONFIG, "broker.host")
            .withProperty(targetProperty, "a value")
            .withProperty(propertyToOverride, "a target value")
            .withProperty(notProducerConfigTargetProperty, "to filter")
            .build();

        Map<String, String> properties = Map.of(
            inputProperty, "a VALUE",
            propertyToOverride, "a property value",
            notProducerConfigInputProperty, "to filter"
        );

        Map<String, String> expectedConfig = Map.of(
            targetProperty, "a value",
            inputProperty, "a VALUE",
            propertyToOverride, "a property value"
        );

        var sut = new ChutneyKafkaProducerFactory();
        var kafkaTemplate = sut.create(target, properties);

        assertThat(kafkaTemplate.getProducerFactory().getConfigurationProperties())
            .containsAllEntriesOf(expectedConfig)
            .doesNotContainKeys(notProducerConfigTargetProperty, notProducerConfigInputProperty)
        ;
    }
}
