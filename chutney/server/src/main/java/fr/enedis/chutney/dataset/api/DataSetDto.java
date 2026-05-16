/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDataSetDto.class)
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

    @JsonCreator
    static DataSetDto of(
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("name") String name,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("lastUpdated") @Nullable Instant lastUpdated,
        @JsonProperty("tags") @Nullable List<String> tags,
        @JsonProperty("uniqueValues") @Nullable List<KeyValue> constants,
        @JsonProperty("multipleValues") @Nullable List<List<KeyValue>> datatable,
        @JsonProperty("scenarioUsage") @Nullable List<String> scenarioUsage,
        @JsonProperty("campaignUsage") @Nullable List<String> campaignUsage,
        @JsonProperty("scenarioInCampaignUsage") @Nullable Map<String, Set<String>> scenarioInCampaignUsage
    ) {
        ImmutableDataSetDto.Builder builder = ImmutableDataSetDto.builder().name(name);
        if (id != null) builder.id(id);
        if (description != null) builder.description(description);
        if (lastUpdated != null) builder.lastUpdated(lastUpdated);
        if (tags != null) builder.tags(tags);
        if (constants != null) builder.constants(constants);
        if (datatable != null) builder.datatable(datatable);
        if (scenarioUsage != null) builder.scenarioUsage(scenarioUsage);
        if (campaignUsage != null) builder.campaignUsage(campaignUsage);
        if (scenarioInCampaignUsage != null) builder.scenarioInCampaignUsage(scenarioInCampaignUsage);
        return builder.build();
    }
}
