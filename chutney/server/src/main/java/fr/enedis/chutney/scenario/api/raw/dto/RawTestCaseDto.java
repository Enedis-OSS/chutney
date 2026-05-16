/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api.raw.dto;

import static java.time.Instant.now;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.enedis.chutney.server.core.domain.security.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableRawTestCaseDto.class)
@Value.Style(jdkOnly = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface RawTestCaseDto {

    @JsonProperty("content")
    String scenario();

    @JsonProperty("id")
    Optional<String> id();

    @JsonProperty("title")
    String title();

    Optional<String> description();

    List<String> tags();

    Optional<String> defaultDataset();

    @Value.Default()
    default Instant creationDate() {
        return now();
    }

    @Value.Default()
    default String author() {
        return User.ANONYMOUS.id;
    }

    @Value.Default()
    default Instant updateDate() {
        return now();
    }

    @Value.Default()
    default Integer version() {
        return 1;
    }

    @JsonCreator
    static RawTestCaseDto of(
        @JsonProperty("content") @JsonAlias("scenario") String scenario,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("title") String title,
        @JsonProperty("description") @Nullable String description,
        @JsonProperty("tags") @Nullable List<String> tags,
        @JsonProperty("defaultDataset") @Nullable String defaultDataset,
        @JsonProperty("creationDate") @Nullable Instant creationDate,
        @JsonProperty("author") @Nullable String author,
        @JsonProperty("updateDate") @Nullable Instant updateDate,
        @JsonProperty("version") @Nullable Integer version
    ) {
        ImmutableRawTestCaseDto.Builder builder = ImmutableRawTestCaseDto.builder()
            .title(title)
            .scenario(scenario);
        if (id != null) builder.id(id);
        if (description != null) builder.description(description);
        if (tags != null) builder.tags(tags);
        if (defaultDataset != null) builder.defaultDataset(defaultDataset);
        if (creationDate != null) builder.creationDate(creationDate);
        if (author != null) builder.author(author);
        if (updateDate != null) builder.updateDate(updateDate);
        if (version != null) builder.version(version);
        return builder.build();
    }
}
