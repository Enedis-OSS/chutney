/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka;

import static fr.enedis.chutney.action.kafka.KafkaClientFactoryHelper.filterMapFrom;
import static fr.enedis.chutney.action.kafka.KafkaClientFactoryHelper.resolveBootStrapServerConfig;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;

import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.exec.util.MapUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

final class ChutneyKafkaProducerFactory {

    private DefaultKafkaProducerFactory<String, ?> factory;

    KafkaTemplate<String, ?> create(Target target, Map<String, String> config) {

        var producerConfigInput = filterAndMergeProperties(target, config);

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

    private static Map<String, String> filterAndMergeProperties(Target target, Map<String, String> config) {
        Set<String> producerConfigKeys = ProducerConfig.configDef().configKeys().keySet();
        return MapUtils.merge(
            filterMapFrom(producerConfigKeys, target.prefixedProperties("")),
            filterMapFrom(producerConfigKeys, config)
        );
    }
}
