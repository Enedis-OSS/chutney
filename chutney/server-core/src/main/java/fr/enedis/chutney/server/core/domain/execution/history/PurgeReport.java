/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution.history;

import java.util.Set;

public record PurgeReport(Set<Long> scenariosExecutionsIds, Set<Long> campaignsExecutionsIds) {
    public PurgeReport {
        scenariosExecutionsIds = Set.copyOf(scenariosExecutionsIds);
        campaignsExecutionsIds = Set.copyOf(campaignsExecutionsIds);
    }
}
