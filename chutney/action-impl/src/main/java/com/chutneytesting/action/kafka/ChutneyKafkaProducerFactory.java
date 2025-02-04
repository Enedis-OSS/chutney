/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.kafka;

import static com.chutneytesting.action.kafka.KafkaClientFactoryHelper.resolveBootStrapServerConfig;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;

import com.chutneytesting.action.spi.injectable.Target;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.exec.util.MapUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

final class ChutneyKafkaProducerFactory {

    private DefaultKafkaProducerFactory<String, ?> factory;

    KafkaTemplate<String, ?> create(Target target, Map<String, String> config) {

        var producerConfigInput = MapUtils.merge(filterProducerConfig(target), filterProducerConfig(config));

        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(BOOTSTRAP_SERVERS_CONFIG, resolveBootStrapServerConfig(target));
        // Allow bootstrap servers config override
        producerConfig.putAll(producerConfigInput);
        // Set default serializers
        producerConfig.putIfAbsent(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.putIfAbsent(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        // Override SSL truststore config from target
        target.trustStore().ifPresent(trustStore -> {
            producerConfig.put(SSL_TRUSTSTORE_LOCATION_CONFIG, trustStore);
            target.trustStorePassword().ifPresent(trustStorePassword ->
                producerConfig.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword)
            );
        });

        this.factory = new DefaultKafkaProducerFactory<>(producerConfig);
        return new KafkaTemplate<>(this.factory, true);
    }

    void destroy() {
        factory.destroy();
    }

    private Map<String, String> filterProducerConfig(Target target) {
        if (target != null) {
            Map<String, String> config = new HashMap<>();
            ProducerConfig.configDef().configKeys().keySet().forEach(ck ->
                target.property(ck).ifPresent(cv -> config.put(ck, cv))
            );
            return unmodifiableMap(config);
        }
        return emptyMap();
    }

    private Map<String, String> filterProducerConfig(Map<String, String> config) {
        if (config != null) {
            Map<String, String> resultConfig = new HashMap<>();
            ProducerConfig.configDef().configKeys().keySet().forEach(ck -> {
                if (config.containsKey(ck)) {
                    resultConfig.put(ck, config.get(ck));
                }
            });
            return unmodifiableMap(resultConfig);
        }
        return emptyMap();
    }
}
