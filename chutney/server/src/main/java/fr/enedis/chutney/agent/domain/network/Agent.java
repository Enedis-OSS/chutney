/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.network;

import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Agent {

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    public final NamedHostAndPort agentInfo; // TODO any - why NamedHostAndPort does not have an AgentId + why member is agentInfo which sounds like Target which has the right to be a real class ?
    private final Set<Agent> reachableAgents = new LinkedHashSet<>(); // TODO any - why have Agent and not AgentId
    private final Set<TargetId> reachableTargets = new LinkedHashSet<>(); // TODO any - why have TargetId and not Target
    // TODO any - all these words, attribute name and classes are not coherent and are mostly confusing

    public Agent(NamedHostAndPort agentInfo) {
        this.agentInfo = agentInfo;
    }

    public Agent addReachable(Agent agent) {
        reachableAgents.add(agent);
        return this;
    }

    public Set<Agent> reachableAgents() {
        return new LinkedHashSet<>(reachableAgents);
    }

    public Agent addReachable(TargetId target) {
        reachableTargets.add(target);
        return this;
    }

    public Set<TargetId> reachableTargets() {
        return new LinkedHashSet<>(reachableTargets);
    }

    public List<Agent> findFellowAgentForReaching(String targetName, String environment) {
        List<Agent> result = new ArrayList<>();
        Optional<Agent> resultFound = findNext(this, TargetId.of(targetName, environment),  Sets.newHashSet(this), result);
        if (resultFound.isPresent()) {
            Collections.reverse(result);
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private Optional<Agent> findNext(Agent agent, TargetId target, Set<Agent> scannedAgents, List<Agent> accumulator) {

        if (agent.reachableTargets().contains(target)) {
            LOGGER.debug("Target reachable by agent " + agent.agentInfo.name());
            return Optional.of(agent);
        }
        LOGGER.debug("Target NOT reachable by agent " + agent.agentInfo.name());
        scannedAgents.add(agent);

        Optional<Agent> agentFound = agent.reachableAgents.stream()
            .filter(nextAgent -> !scannedAgents.contains(nextAgent))
            .filter(nextAgent -> findNext(nextAgent, target, Sets.newHashSet(scannedAgents), accumulator).isPresent())
            .findFirst();

        if (agentFound.isPresent()) {
            LOGGER.debug("Next Agent found " + agentFound.get().agentInfo.name());
            accumulator.add(agentFound.get());
        } else {
            LOGGER.debug("No next agent found. Will try to execute with local agent.");
        }
        return agentFound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent agent = (Agent) o;
        return Objects.equals(agentInfo, agent.agentInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentInfo);
    }

    @Override
    public String toString() {
        return "Agent: " + agentInfo.toString();
    }
}
