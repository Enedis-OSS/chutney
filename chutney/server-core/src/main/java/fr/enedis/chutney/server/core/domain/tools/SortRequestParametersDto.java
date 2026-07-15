/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableSortRequestParametersDto.class)
@JsonDeserialize(as = ImmutableSortRequestParametersDto.class)
@Value.Style(jdkOnly = true)
public interface SortRequestParametersDto {

    @JsonProperty("sort")
    @Nullable
    String sort();

    @JsonProperty("desc")
    @Nullable
    String desc();

    @JsonIgnore
    @Value.Derived
    default List<String> sortParameters() {
        return sort() != null ? Arrays.asList(sort().split(",")) : Collections.emptyList();
    }

    @JsonIgnore
    @Value.Derived
    default List<String> descParameters() {
        return desc() != null ? (desc().length() > 0 ? Arrays.asList(desc().split(",")) : sortParameters()) : Collections.emptyList();
    }

    @JsonCreator
    static SortRequestParametersDto of(
        @JsonProperty("sort") @Nullable String sort,
        @JsonProperty("desc") @Nullable String desc
    ) {
        ImmutableSortRequestParametersDto.Builder builder = ImmutableSortRequestParametersDto.builder();
        if (sort != null) builder.sort(sort);
        if (desc != null) builder.desc(desc);
        return builder.build();
    }
}
