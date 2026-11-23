/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableJiraDto.class)
@Value.Style(jdkOnly = true)
public interface JiraDto {

    @JsonProperty("id")
    String id();

    @JsonProperty("chutneyId")
    String chutneyId();

    @JsonProperty("executionStatus")
    Optional<String> executionStatus();

    @JsonProperty("datasetId")
    Optional<String> datasetId();

    @JsonCreator
    static JiraDto of(
        @JsonProperty("id") String id,
        @JsonProperty("chutneyId") String chutneyId,
        @JsonProperty("executionStatus") @Nullable String executionStatus,
        @JsonProperty("datasetId") @Nullable String datasetId
    ) {
        ImmutableJiraDto.Builder builder = ImmutableJiraDto.builder().id(id).chutneyId(chutneyId);
        if (executionStatus != null) builder.executionStatus(executionStatus);
        if (datasetId != null) builder.datasetId(datasetId);
        return builder.build();
    }
}
