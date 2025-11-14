/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.security;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;

public enum Authorization {

    SCENARIO_READ,
    SCENARIO_WRITE,

    CAMPAIGN_READ,
    CAMPAIGN_WRITE,

    EXECUTION_READ,
    EXECUTION_WRITE,

    ENVIRONMENT_READ,
    ENVIRONMENT_WRITE,

    TARGET_READ,
    TARGET_WRITE,

    VARIABLE_READ,
    VARIABLE_WRITE,

    DATASET_READ,
    DATASET_WRITE,

    ADMIN_ACCESS;

    private static final String WRITE_SUFFIX = "_WRITE";
    private static final String READ_SUFFIX = "_READ";

    public Optional<Authorization> readAuthorization() {
        if (isWriteAuthorization()) {
            return of(
                Authorization.valueOf(this.name().replace(WRITE_SUFFIX, READ_SUFFIX))
            );
        }
        return empty();
    }

    public boolean isWriteAuthorization() {
        return this.name().endsWith(WRITE_SUFFIX);
    }
}
