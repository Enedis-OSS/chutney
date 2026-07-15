/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutablePaginationRequestParametersDto.class)
@JsonDeserialize(as = ImmutablePaginationRequestParametersDto.class)
@Value.Style(jdkOnly = true)
public interface PaginationRequestParametersDto {

    @JsonProperty("start")
    @Value.Default
    default Long start() { return 1L; }

    @JsonProperty("limit")
    @Value.Default
    default Long limit() { return 25L; }

    @JsonCreator
    static PaginationRequestParametersDto of(
        @JsonProperty("start") @Nullable Long start,
        @JsonProperty("limit") @Nullable Long limit
    ) {
        ImmutablePaginationRequestParametersDto.Builder builder = ImmutablePaginationRequestParametersDto.builder();
        if (start != null) builder.start(start);
        if (limit != null) builder.limit(limit);
        return builder.build();
    }
}
