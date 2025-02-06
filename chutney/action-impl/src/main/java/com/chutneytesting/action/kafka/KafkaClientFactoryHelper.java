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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class KafkaClientFactoryHelper {

    static String resolveBootStrapServerConfig(Target target) {
        return target.property(BOOTSTRAP_SERVERS_CONFIG)
            .or(() -> of(target.uri()).map(URI::getAuthority))
            .orElseGet(() -> target.uri().toString());
    }

    static Map<String, String> filterMapFrom(Set<String> keySet, Map<String, String> mapToFilter) {
        if (mapToFilter != null) {
            return mapToFilter.entrySet().stream()
                .filter(e -> keySet.contains(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return emptyMap();
    }
}
