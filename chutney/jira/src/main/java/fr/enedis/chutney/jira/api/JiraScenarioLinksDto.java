/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableJiraScenarioLinksDto.class)
@Value.Style(jdkOnly = true)
public interface JiraScenarioLinksDto {

    @JsonProperty("id")
    @Nullable
    String id();

    @JsonProperty("chutneyId")
    String chutneyId();

    @JsonProperty("datasetLinks")
    @Value.Default
    default Map<String, String> datasetLinks() {
        return emptyMap();
    }

    @JsonCreator
    static JiraScenarioLinksDto of(
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("chutneyId") String chutneyId,
        @JsonProperty("datasetLinks") @Nullable Map<String, String> datasetLinks
    ) {
        ImmutableJiraScenarioLinksDto.Builder builder = ImmutableJiraScenarioLinksDto.builder()
            .id(id)
            .chutneyId(chutneyId);
        if (datasetLinks != null) builder.datasetLinks(datasetLinks);
        return builder.build();
    }
}
