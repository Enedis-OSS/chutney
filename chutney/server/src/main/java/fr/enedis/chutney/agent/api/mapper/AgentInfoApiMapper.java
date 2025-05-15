/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.mapper;

import fr.enedis.chutney.agent.api.dto.NetworkConfigurationApiDto;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import org.springframework.stereotype.Component;

@Component
public class AgentInfoApiMapper {

    public NetworkConfigurationApiDto.AgentInfoApiDto toDto(NamedHostAndPort agentInfo) {
        NetworkConfigurationApiDto.AgentInfoApiDto dto = new NetworkConfigurationApiDto.AgentInfoApiDto();
        dto.name = agentInfo.name();
        dto.host = agentInfo.host();
        dto.port = agentInfo.port();
        return dto;
    }

    public NamedHostAndPort fromDto(NetworkConfigurationApiDto.AgentInfoApiDto entity) {
        return new NamedHostAndPort(entity.name, entity.host, entity.port);
    }
}
