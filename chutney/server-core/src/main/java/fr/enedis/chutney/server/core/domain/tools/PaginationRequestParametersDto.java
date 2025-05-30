/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePaginationRequestParametersDto.class)
@JsonDeserialize(as = ImmutablePaginationRequestParametersDto.class)
@Value.Style(jdkOnly = true)
public interface PaginationRequestParametersDto {

    @Value.Default
    default Long start() { return 1L; }

    @Value.Default
    default Long limit() { return 25L; }
}
