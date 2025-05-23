/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.dto;

import fr.enedis.chutney.agent.domain.network.Agent;
import java.util.List;

/**
 * DTO for {@link Agent} transport.
 */
public class AgentApiDto {
    public NetworkConfigurationApiDto.AgentInfoApiDto info;
    public List<String> reachableAgents;
    public List<TargetIdEntity> reachableTargets;
}
