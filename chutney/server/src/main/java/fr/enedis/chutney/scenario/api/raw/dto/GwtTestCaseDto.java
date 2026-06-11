/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import static java.time.Instant.now;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.enedis.chutney.execution.api.ExecutionSummaryDto;
import fr.enedis.chutney.server.core.domain.security.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableGwtTestCaseDto.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value.Style(jdkOnly = true)
public interface GwtTestCaseDto {

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
    Optional<Instant> creationDate();

    @JsonProperty("scenario")
    GwtScenarioDto scenario();

    @JsonProperty("defaultDataset")
    Optional<String> defaultDataset();

    @JsonProperty("author")
    @Value.Default()
    default String author() {
        return User.ANONYMOUS.id;
    }

    @JsonProperty("updateDate")
    @Value.Default()
    default Instant updateDate() {
        return now();
    }

    @JsonProperty("version")
    @Value.Default()
    default Integer version() {
        return 1;
    }

    @JsonCreator
    static GwtTestCaseDto of(
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("title") String title,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("repositorySource") @Nullable String repositorySource,
        @JsonProperty("tags") @Nullable List<String> tags,
        @JsonProperty("executions") @Nullable List<ExecutionSummaryDto> executions,
        @JsonProperty("creationDate") @Nullable Instant creationDate,
        @JsonProperty("scenario") GwtScenarioDto scenario,
        @JsonProperty("defaultDataset") @Nullable String defaultDataset,
        @JsonProperty("author") @Nullable String author,
        @JsonProperty("updateDate") @Nullable Instant updateDate,
        @JsonProperty("version") @Nullable Integer version
    ) {
        ImmutableGwtTestCaseDto.Builder builder = ImmutableGwtTestCaseDto.builder()
            .title(title)
            .scenario(scenario);
        if (id != null) builder.id(id);
        if (description != null) builder.description(description);
        if (repositorySource != null) builder.repositorySource(repositorySource);
        if (tags != null) builder.tags(tags);
        if (executions != null) builder.executions(executions);
        if (creationDate != null) builder.creationDate(creationDate);
        if (defaultDataset != null) builder.defaultDataset(defaultDataset);
        if (author != null) builder.author(author);
        if (updateDate != null) builder.updateDate(updateDate);
        if (version != null) builder.version(version);
        return builder.build();
    }
}
