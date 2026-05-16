/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

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
