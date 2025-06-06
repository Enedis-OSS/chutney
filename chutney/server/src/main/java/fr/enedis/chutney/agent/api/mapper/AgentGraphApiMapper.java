/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.mapper;

import fr.enedis.chutney.agent.api.dto.AgentApiDto;
import fr.enedis.chutney.agent.api.dto.AgentsGraphApiDto;
import fr.enedis.chutney.agent.api.dto.TargetIdEntity;
import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.agent.domain.network.Agent;
import fr.enedis.chutney.agent.domain.network.AgentGraph;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AgentGraphApiMapper {

    private final AgentInfoApiMapper agentInfoApiMapper;

    public AgentGraphApiMapper(AgentInfoApiMapper agentInfoApiMapper) {
        this.agentInfoApiMapper = agentInfoApiMapper;
    }

    public AgentsGraphApiDto toDto(AgentGraph agentGraph) {
        AgentsGraphApiDto dto = new AgentsGraphApiDto();

        dto.agents = agentGraph.agents().stream().map(this::toDto).collect(Collectors.toList());

        return dto;
    }

    private AgentApiDto toDto(Agent agent) {
        AgentApiDto dto = new AgentApiDto();

        dto.info = agentInfoApiMapper.toDto(agent.agentInfo);
        dto.reachableAgents = agent.reachableAgents().stream().map(a -> a.agentInfo.name()).collect(Collectors.toList());
        dto.reachableTargets = agent.reachableTargets().stream().map(t -> new TargetIdEntity(t.name, t.environment)).collect(Collectors.toList());

        return dto;
    }

    public AgentGraph fromDto(AgentsGraphApiDto agentsGraph) {
        Map<String, Agent> agents = agentsGraph.agents.stream()
            .map(agent -> agent.info)
            .map(agentInfoApiMapper::fromDto)
            .map(Agent::new)
            .collect(Collectors.toMap(agent -> agent.agentInfo.name(), Function.identity()));

        for (AgentApiDto agentApiDto : agentsGraph.agents) {
            Agent agent = agents.get(agentApiDto.info.name);

            for (String remote : agentApiDto.reachableAgents)
                agent.addReachable(agents.get(remote));

            for (TargetIdEntity targetIdEntity : agentApiDto.reachableTargets)
                agent.addReachable(fromDto(targetIdEntity));
        }


        return new AgentGraph(agents.values());
    }

    private TargetId fromDto(TargetIdEntity targetIdEntity) {
        return TargetId.of(targetIdEntity.name, targetIdEntity.environment);
    }
}
