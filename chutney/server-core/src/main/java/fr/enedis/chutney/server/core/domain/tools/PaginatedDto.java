/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.tools;

import java.util.Collections;
import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutablePaginatedDto.class)
@JsonDeserialize(as = ImmutablePaginatedDto.class)
@Value.Style(jdkOnly = true)
public interface PaginatedDto<PAGINATED_OBJECT> {

    long totalCount();
    @Value.Default
    default List<PAGINATED_OBJECT> data() { return Collections.emptyList(); }
}
