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

    @JsonProperty("description")
    Optional<String> description();

    @JsonProperty("repositorySource")
    Optional<String> repositorySource();

    @JsonProperty("tags")
    List<String> tags();

    @JsonProperty("executions")
    List<ExecutionSummaryDto> executions();

    @JsonProperty("creationDate")
    @Value.Default()
    default Instant creationDate() {
        return Instant.now();
    }

    @JsonProperty("updateDate")
    @Value.Default()
    default Instant updateDate() {
        return Instant.now();
    }

    @JsonProperty("author")
    Optional<String> author();

    @JsonProperty("version")
    Optional<Integer> version();

    @JsonCreator
    static GwtTestCaseMetadataDto of(
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("title") String title,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("repositorySource") @Nullable String repositorySource,
        @JsonProperty("tags") @Nullable List<String> tags,
        @JsonProperty("executions") @Nullable List<ExecutionSummaryDto> executions,
        @JsonProperty("creationDate") @Nullable Instant creationDate,
        @JsonProperty("updateDate") @Nullable Instant updateDate,
        @JsonProperty("author") @Nullable String author,
        @JsonProperty("version") @Nullable Integer version
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
        if (author != null) builder.author(author);
        if (version != null) builder.version(version);
        return builder.build();
    }
}
