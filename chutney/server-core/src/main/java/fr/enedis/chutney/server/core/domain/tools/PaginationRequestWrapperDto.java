/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutablePaginationRequestWrapperDto.class)
@JsonDeserialize(as = ImmutablePaginationRequestWrapperDto.class)
@Value.Style(jdkOnly = true)
public interface PaginationRequestWrapperDto<W> {

    @JsonProperty("pageNumber")
    Integer pageNumber();

    @JsonProperty("elementPerPage")
    Integer elementPerPage();

    @JsonProperty("wrappedRequest")
    @Value.Default
    default Optional<W> wrappedRequest() { return Optional.empty(); }

    @JsonCreator
    static <W> PaginationRequestWrapperDto<W> of(
        @JsonProperty("pageNumber") Integer pageNumber,
        @JsonProperty("elementPerPage") Integer elementPerPage,
        @JsonProperty("wrappedRequest") @Nullable W wrappedRequest
    ) {
        ImmutablePaginationRequestWrapperDto.Builder<W> builder = ImmutablePaginationRequestWrapperDto.<W>builder()
            .pageNumber(pageNumber)
            .elementPerPage(elementPerPage);
        if (wrappedRequest != null) builder.wrappedRequest(wrappedRequest);
        return builder.build();
    }
}
