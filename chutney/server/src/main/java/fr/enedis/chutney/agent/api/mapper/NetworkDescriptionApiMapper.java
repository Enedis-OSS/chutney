/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.mapper;

import fr.enedis.chutney.agent.api.dto.NetworkDescriptionApiDto;
import fr.enedis.chutney.agent.domain.network.ImmutableNetworkDescription;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import org.springframework.stereotype.Component;

@Component
public class NetworkDescriptionApiMapper {
    private final NetworkConfigurationApiMapper networkConfigurationMapper;
    private final AgentGraphApiMapper agentGraphMapper;

    public NetworkDescriptionApiMapper(NetworkConfigurationApiMapper networkConfigurationMapper, AgentGraphApiMapper agentGraphMapper) {
        this.networkConfigurationMapper = networkConfigurationMapper;
        this.agentGraphMapper = agentGraphMapper;
    }

    public NetworkDescriptionApiDto toDto(NetworkDescription networkDescription) {
        NetworkDescriptionApiDto networkDescriptionApiDto = new NetworkDescriptionApiDto();
        networkDescriptionApiDto.agentsGraph = agentGraphMapper.toDto(networkDescription.agentGraph());
        networkDescriptionApiDto.networkConfiguration = networkConfigurationMapper.toDto(networkDescription.configuration());
        return networkDescriptionApiDto;
    }

    public NetworkDescription fromDto(NetworkDescriptionApiDto networkDescriptionApiDto) {
        return ImmutableNetworkDescription.builder()
            .agentGraph(agentGraphMapper.fromDto(networkDescriptionApiDto.agentsGraph))
            .configuration(networkConfigurationMapper.fromDto(networkDescriptionApiDto.networkConfiguration))
            .build();
    }
}
