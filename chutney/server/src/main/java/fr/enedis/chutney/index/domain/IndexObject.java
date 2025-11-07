/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.domain;

import fr.enedis.chutney.server.core.domain.security.Authorization;
import java.util.Optional;

public enum IndexObject {
    SCENARIO,
    CAMPAIGN,
    DATASET;

    public static Optional<IndexObject> fromAuthorization(Authorization authorization) {
        return Optional.ofNullable(
            switch (authorization) {
                case SCENARIO_READ, SCENARIO_WRITE -> IndexObject.SCENARIO;
                case CAMPAIGN_READ, CAMPAIGN_WRITE -> IndexObject.CAMPAIGN;
                case DATASET_READ, DATASET_WRITE -> IndexObject.DATASET;
                default -> null;
            });
    }
}
