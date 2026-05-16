/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtTestCaseMetadataDto.class)
public interface GwtTestCaseMetadataDto {

    @JsonProperty("id")
    Optional<String> id();

    @JsonProperty("title")
    String title();

    Optional<String> description();

    Optional<String> repositorySource();

    List<String> tags();

    List<ExecutionSummaryDto> executions();

    @Value.Default()
    default Instant creationDate() {
        return Instant.now();
    }

    @Value.Default()
    default Instant updateDate() {
        return Instant.now();
    }

    @JsonCreator
    static GwtTestCaseMetadataDto of(
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("title") String title,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("repositorySource") @Nullable String repositorySource,
        @JsonProperty("tags") @Nullable List<String> tags,
        @JsonProperty("executions") @Nullable List<ExecutionSummaryDto> executions,
        @JsonProperty("creationDate") @Nullable Instant creationDate,
        @JsonProperty("updateDate") @Nullable Instant updateDate
    ) {
        ImmutableGwtTestCaseMetadataDto.Builder builder = ImmutableGwtTestCaseMetadataDto.builder()
            .title(title);
        if (id != null) builder.id(id);
        if (description != null) builder.description(description);
        if (repositorySource != null) builder.repositorySource(repositorySource);
        if (tags != null) builder.tags(tags);
        if (executions != null) builder.executions(executions);
        if (creationDate != null) builder.creationDate(creationDate);
        if (updateDate != null) builder.updateDate(updateDate);
        return builder.build();
    }
}
