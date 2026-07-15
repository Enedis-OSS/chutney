/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutablePaginatedDto.class)
@JsonDeserialize(as = ImmutablePaginatedDto.class)
@Value.Style(jdkOnly = true)
public interface PaginatedDto<PAGINATED_OBJECT> {

    @JsonProperty("totalCount")
    long totalCount();

    @JsonProperty("data")
    @Value.Default
    default List<PAGINATED_OBJECT> data() { return Collections.emptyList(); }

    @JsonCreator
    static <T> PaginatedDto<T> of(
        @JsonProperty("totalCount") long totalCount,
        @JsonProperty("data") @Nullable List<T> data
    ) {
        ImmutablePaginatedDto.Builder<T> builder = ImmutablePaginatedDto.<T>builder().totalCount(totalCount);
        if (data != null) builder.data(data);
        return builder.build();
    }
}
