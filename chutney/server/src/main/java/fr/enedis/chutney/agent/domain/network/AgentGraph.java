/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.network;

import static java.util.stream.Collectors.toMap;

import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.explore.AgentId;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.explore.ExploreResult.Link;
import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;

public class AgentGraph {

    public static AgentGraph of(ExploreResult exploreResult, NetworkConfiguration networkConfiguration) {
        Map<String, Agent> agentsByName = indexAgents(networkConfiguration, agent -> agent.agentInfo.name());
        Map<String, Agent> agentsByHost = indexAgents(networkConfiguration, agent -> agent.agentInfo.host());

        for (Link<AgentId, AgentId> agentLink : exploreResult.agentLinks()) {
            Agent sourceAgent = searchInConfiguration(agentsByName, agentsByHost, agentLink.source());
            Agent destAgent = searchInConfiguration(agentsByName, agentsByHost, agentLink.destination());
            if (sourceAgent == null) {
                throw new IllegalStateException(String.format("the agent [%s] is declared as source but does not exist in configuration", agentLink.source()));
            }
            if (destAgent == null) {
                throw new IllegalStateException(String.format("the agent [%s] is declared as destination but does not exist in configuration", agentLink.destination()));
            }

            sourceAgent.addReachable(destAgent);
        }

        Map<TargetId, Pair<String, TargetDto>> targetById = networkConfiguration.environmentConfiguration().stream()
            .flatMap(e -> e.targets.stream().map(t -> Pair.of(e.name, t)))
            .collect(toMap(p -> TargetId.of(p.getRight().name, p.getLeft()), Function.identity()));

        for (Link<AgentId, TargetId> targetLink : exploreResult.targetLinks()) {
            Agent sourceAgent = agentsByName.get(targetLink.source().name());
            Pair<String, TargetDto> destTarget = targetById.get(targetLink.destination());
            if (sourceAgent == null)
                throw new IllegalStateException(String.format("the agent [%s] is declared as source but does not exist in configuration", targetLink.source()));
            if (destTarget == null)
                throw new IllegalStateException(String.format("the target [%s] is declared as destination but does not exist in configuration", targetLink.destination()));
            sourceAgent.addReachable(TargetId.of(destTarget.getRight().name, destTarget.getLeft()));
        }

        return new AgentGraph(new HashSet<>(agentsByName.values()));
    }

    private static Agent searchInConfiguration(Map<String, Agent> agentsByName, Map<String, Agent> agentsByHost, AgentId agentId) {
        return agentsByName.getOrDefault(agentId.name(), agentsByHost.get(agentId.name()));
    }

    private static Map<String, Agent> indexAgents(NetworkConfiguration networkConfiguration, Function<Agent, String> indexFunction) {
        return networkConfiguration.agentNetworkConfiguration().stream()
            .map(Agent::new)
            .collect(toMap(indexFunction, Function.identity()));
    }

    private final Set<Agent> agents;

    public AgentGraph(Collection<Agent> agents) {
        this.agents = new LinkedHashSet<>(agents);
    }

    public Set<Agent> agents() {
        return new LinkedHashSet<>(agents);
    }

    Optional<Agent> getBy(AgentId agentId) {
        return agents.stream()
            .filter(agent -> agent.agentInfo.name().equals(agentId.name()))
            .findFirst();
    }
}
