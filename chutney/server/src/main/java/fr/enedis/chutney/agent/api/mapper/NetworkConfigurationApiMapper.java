/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.mapper;

import static java.util.stream.Collectors.toSet;

import fr.enedis.chutney.agent.api.dto.NetworkConfigurationApiDto;
import fr.enedis.chutney.agent.domain.configure.ImmutableNetworkConfiguration;
import fr.enedis.chutney.agent.domain.configure.ImmutableNetworkConfiguration.AgentNetworkConfiguration;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NetworkConfigurationApiMapper {

    private final AgentInfoApiMapper agentInfoMapper;
    private final EnvironmentApiMapper targetApiMapper;

    public NetworkConfigurationApiMapper(AgentInfoApiMapper agentInfoMapper, EnvironmentApiMapper targetApiMapper) {
        this.agentInfoMapper = agentInfoMapper;
        this.targetApiMapper = targetApiMapper;
    }

    public NetworkConfigurationApiDto toDto(NetworkConfiguration networkConfiguration) {
        NetworkConfigurationApiDto dto = new NetworkConfigurationApiDto();

        dto.creationDate = networkConfiguration.creationDate();
        dto.agentNetworkConfiguration = networkConfiguration.agentNetworkConfiguration().agentInfos().stream()
            .map(agentInfoMapper::toDto)
            .collect(toSet());

        dto.environmentsConfiguration = networkConfiguration.environmentConfiguration().stream()
            .map(targetApiMapper::toDto)
            .collect(toSet());

        return dto;
    }

    public NetworkConfiguration fromDto(NetworkConfigurationApiDto dto) {
        return fromDtoAt(dto, dto.creationDate);
    }

    public NetworkConfiguration fromDtoAtNow(NetworkConfigurationApiDto dto) {
        return fromDtoAt(dto, Instant.now());
    }

    private NetworkConfiguration fromDtoAt(NetworkConfigurationApiDto entity, Instant creation) {
        return ImmutableNetworkConfiguration.builder()
            .agentNetworkConfiguration(
                AgentNetworkConfiguration.builder()
                    .agentInfos(
                        entity.agentNetworkConfiguration.stream()
                            .map(agentInfoMapper::fromDto)
                            .collect(toSet()))

                    .build())
            .environmentConfiguration(ImmutableNetworkConfiguration.EnvironmentConfiguration.builder()
                .environments(entity.environmentsConfiguration.stream().map(targetApiMapper::fromDto).collect(toSet()))
                .build())
            .creationDate(creation)
            .build();
    }

    public NetworkConfiguration enhanceWithEnvironment(NetworkConfiguration networkConfiguration, Set<EnvironmentDto> localEnvironments) {
        return ImmutableNetworkConfiguration.builder()
            .from(networkConfiguration)
            .environmentConfiguration(ImmutableNetworkConfiguration.EnvironmentConfiguration.builder()
                .addAllEnvironments(localEnvironments)
                .build())
            .build();
    }
}
