/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.security;

public enum Authorization {

    SCENARIO_READ,
    SCENARIO_WRITE,

    CAMPAIGN_READ,
    CAMPAIGN_WRITE,

    EXECUTION_READ,
    EXECUTION_WRITE,

    ENVIRONMENT_ACCESS,

    DATASET_READ,
    DATASET_WRITE,

    ADMIN_ACCESS
}
