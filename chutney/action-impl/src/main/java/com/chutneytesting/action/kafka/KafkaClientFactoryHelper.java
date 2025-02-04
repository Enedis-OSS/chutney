/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.kafka;

import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

import com.chutneytesting.action.spi.injectable.Target;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

final class KafkaClientFactoryHelper {

    static String resolveBootStrapServerConfig(Target target) {
        return target.property(BOOTSTRAP_SERVERS_CONFIG)
            .or(() -> of(target.uri()).map(URI::getAuthority))
            .orElseGet(() -> target.uri().toString());
    }

    static Map<String, String> buildFilteredMapFrom(
        Object checkNullObject,
        Set<String> set, BiConsumer<String, Map<String, String>> consumer
    ) {
        if (checkNullObject != null) {
            Map<String, String> result = new HashMap<>();
            set.forEach(k -> consumer.accept(k, result));
            return result;
        }
        return emptyMap();
    }
}
