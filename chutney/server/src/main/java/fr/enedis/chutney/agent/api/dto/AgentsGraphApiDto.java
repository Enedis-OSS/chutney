/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.dto;

import fr.enedis.chutney.agent.domain.network.AgentGraph;
import java.util.List;

/**
 * DTO for {@link AgentGraph} transport.
 */
public class AgentsGraphApiDto {
    public List<AgentApiDto> agents;
}
