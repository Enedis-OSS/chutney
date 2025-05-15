/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.execution;

import fr.enedis.chutney.engine.api.execution.ExecutionRequestDto;
import fr.enedis.chutney.server.core.domain.execution.ExecutionRequest;

public interface ExecutionRequestMapper {

    ExecutionRequestDto toDto(ExecutionRequest executionRequest);

}
