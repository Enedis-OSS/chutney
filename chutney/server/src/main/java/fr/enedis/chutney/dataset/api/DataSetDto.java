/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.api;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDataSetDto.class)
@JsonDeserialize(as = ImmutableDataSetDto.class)
@Value.Style(jdkOnly = true)
public interface DataSetDto {

    Optional<String> id();
    String name();

    @Value.Default()
    default String description() {
        return "";
    }

    @Value.Default()
    default Instant lastUpdated() {
        return now();
    }

    @Value.Default()
    default List<String> tags() {
        return emptyList();
    }

    @Value.Default()
    @JsonProperty("uniqueValues")
    default List<KeyValue> constants() {
        return emptyList();
    }

    @Value.Default()
    @JsonProperty("multipleValues")
    default List<List<KeyValue>> datatable() {
        return emptyList();
    }

    @Value.Default()
    default List<String> scenarioUsage() {
        return emptyList();
    }

    @Value.Default()
    default List<String> campaignUsage() {
        return emptyList();
    }

    @Value.Default()
    default Map<String, Set<String>> scenarioInCampaignUsage() {
        return emptyMap();
    }

    default List<String> duplicatedHeaders() {
        if(!datatable().isEmpty()) {
            List<String> headers = datatable().getFirst().stream().map(KeyValue::key).toList();
            return headers.stream()
                .collect(groupingBy(h -> h, counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        }
        return emptyList();
    }
}
