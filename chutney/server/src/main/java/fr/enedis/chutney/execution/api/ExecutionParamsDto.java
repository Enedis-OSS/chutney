/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api;

import fr.enedis.chutney.dataset.api.ExecutionDatasetDto;

public record ExecutionParamsDto(ExecutionDatasetDto dataset, String jiraId) {
}
