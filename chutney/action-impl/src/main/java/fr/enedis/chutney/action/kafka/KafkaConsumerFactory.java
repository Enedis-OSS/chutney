/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka;

import static fr.enedis.chutney.action.kafka.KafkaClientFactoryHelper.filterMapFrom;
import static fr.enedis.chutney.action.kafka.KafkaClientFactoryHelper.resolveBootStrapServerConfig;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Optional.ofNullable;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;

import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.exec.util.MapUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;

public class KafkaConsumerFactory {

    private static final String AUTO_COMMIT_COUNT_CONFIG = "auto.commit.count";

    MessageListenerContainer create(
        Target target,
        String topic,
        String group,
        Boolean resetOffset,
        String ackMode,
        MessageListener messageListener,
        CommonErrorHandler commonErrorHandler,
        Map<String, String> config
    ) {

        var consumerConfigInput = filterAndMergeProperties(target, config);

        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(BOOTSTRAP_SERVERS_CONFIG, resolveBootStrapServerConfig(target));
        // Allow bootstrap servers config override
        consumerConfig.putAll(consumerConfigInput);

        consumerConfig.put(GROUP_ID_CONFIG, group);
        // Set default deserializers
        consumerConfig.putIfAbsent(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfig.putIfAbsent(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        // Override SSL truststore config from target
        target.trustStore().ifPresent(trustStore -> {
            consumerConfig.put(SSL_TRUSTSTORE_LOCATION_CONFIG, trustStore);
            target.trustStorePassword().ifPresent(trustStorePassword ->
                consumerConfig.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword)
            );
        });

        var kafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfig);

        // Build listener container
        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setMessageListener(messageListener);
        if (resetOffset) {
            containerProperties.setConsumerRebalanceListener(new CustomConsumerRebalanceListener());
        }
        containerProperties.setAckMode(AckMode.valueOf(ackMode));
        ofNullable(consumerConfigInput.get(AUTO_COMMIT_INTERVAL_MS_CONFIG))
            .ifPresent(acims -> containerProperties.setAckTime(parseLong(acims)));
        target.property(AUTO_COMMIT_COUNT_CONFIG)
            .ifPresent(acc -> containerProperties.setAckCount(parseInt(acc)));

        var listenerContainer = new ConcurrentMessageListenerContainer<>(kafkaConsumerFactory, containerProperties);
        listenerContainer.setCommonErrorHandler(commonErrorHandler);
        return listenerContainer;
    }

    private static Map<String, String> filterAndMergeProperties(Target target, Map<String, String> config) {
        Set<String> consumerConfigKeys = ConsumerConfig.configDef().configKeys().keySet();
        return MapUtils.merge(
            filterMapFrom(consumerConfigKeys, target.prefixedProperties("")),
            filterMapFrom(consumerConfigKeys, config)
        );
    }
}
