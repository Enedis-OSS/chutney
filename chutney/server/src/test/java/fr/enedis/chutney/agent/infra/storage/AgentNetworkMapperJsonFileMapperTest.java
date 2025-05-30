/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.agent.domain.configure.ImmutableNetworkConfiguration;
import fr.enedis.chutney.agent.domain.network.Agent;
import fr.enedis.chutney.agent.domain.network.AgentGraph;
import fr.enedis.chutney.agent.domain.network.ImmutableNetworkDescription;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

public class AgentNetworkMapperJsonFileMapperTest {

    private final AgentNetworkMapperJsonFileMapper mapper = new AgentNetworkMapperJsonFileMapper();

    @Test
    public void toDto_should_map_every_information() {
        Agent agent = new Agent(new NamedHostAndPort("name", "host", 42));
        Agent reachableAgent = new Agent(new NamedHostAndPort("reachable", "host2", 42));
        TargetDto target = new TargetDto("targetName", "prot://me:42", emptySet());
        EnvironmentDto environment = new EnvironmentDto("env", null, singletonList(target));
        TargetId targetId = TargetId.of("targetName", "env");
        agent.addReachable(reachableAgent);
        reachableAgent.addReachable(targetId);

        ImmutableNetworkDescription network = ImmutableNetworkDescription.builder()
            .configuration(ImmutableNetworkConfiguration.builder()
                .creationDate(Instant.now())
                .agentNetworkConfiguration(ImmutableNetworkConfiguration.AgentNetworkConfiguration.of(Arrays.asList(agent.agentInfo, reachableAgent.agentInfo)))
                .environmentConfiguration(ImmutableNetworkConfiguration.EnvironmentConfiguration.of(singleton(environment)))
                .build())
            .agentGraph(new AgentGraph(Arrays.asList(agent, reachableAgent)))
            .build();

        AgentNetworkForJsonFile dto = mapper.toDto(network);

        AgentForJsonFile firstAgent = dto.agents.getFirst();
        assertThat(firstAgent.name).isEqualTo("name");
        assertThat(firstAgent.host).isEqualTo("host");
        assertThat(firstAgent.port).isEqualTo(42);
        assertThat(firstAgent.reachableAgentNames).contains("reachable");
        assertThat(firstAgent.reachableTargetIds).isEmpty();

        AgentForJsonFile secondAgent = dto.agents.get(1);
        assertThat(secondAgent.name).isEqualTo("reachable");
        assertThat(secondAgent.host).isEqualTo("host2");
        assertThat(secondAgent.port).isEqualTo(42);
        assertThat(secondAgent.reachableAgentNames).isEmpty();
        assertThat(secondAgent.reachableTargetIds).singleElement().hasFieldOrPropertyWithValue("name", "targetName");
    }

    @Test
    public void fromDto_should_rebuild_everything() {
        String targetName = "targetName";

        TargetForJsonFile target = new TargetForJsonFile();
        target.name = targetName;
        target.environment = "env";

        AgentNetworkForJsonFile networkJson = new AgentNetworkForJsonFile();
        networkJson.configurationCreationDate = Instant.now();
        AgentForJsonFile agent1Json = createAgentJson("agent1", "host1", singletonList("agent2"), emptyList());
        AgentForJsonFile agent2Json = createAgentJson("agent2", "host2", emptyList(), singletonList(target));
        networkJson.agents = Arrays.asList(agent1Json, agent2Json);

        List<TargetDto> targets = new ArrayList<>();
        targets.add(new TargetDto(targetName, "http://s1:90", emptySet()));
        EnvironmentDto environment = new EnvironmentDto("env", null, targets);

        NetworkDescription description = mapper.fromDto(networkJson, singleton(environment));

        assertThat(description.agentGraph().agents()).hasSize(2);
        assertThat(description.agentGraph().agents()).haveAtLeastOne(agentThatMatch(agent1Json));
        assertThat(description.agentGraph().agents()).haveAtLeastOne(agentThatMatch(agent2Json));
    }

    private Condition<Agent> agentThatMatch(AgentForJsonFile expectedAgent) {
        return new Condition<>(actualAgent -> {
            boolean result = actualAgent.agentInfo.name().equals(expectedAgent.name);
            result &= actualAgent.agentInfo.host().equals(expectedAgent.host);
            result &= actualAgent.agentInfo.port() == expectedAgent.port;

            result &= actualAgent.reachableAgents().stream()
                .map(_agent -> _agent.agentInfo.name())
                .allMatch(expectedAgent.reachableAgentNames::contains);

            result &= actualAgent.reachableTargets().stream()
                .allMatch(reachableTarget -> expectedAgent.reachableTargetIds.stream().anyMatch(targetJson ->
                    targetJson.name.equals(reachableTarget.name)));

            return result;
        }, expectedAgent.name);
    }

    private AgentForJsonFile createAgentJson(String agentName, String agentHost, List<String> reachableAgents, List<TargetForJsonFile> reachableTargets) {
        AgentForJsonFile agent1Json = new AgentForJsonFile();
        agent1Json.name = agentName;
        agent1Json.host = agentHost;
        agent1Json.port = 42;
        agent1Json.reachableAgentNames = reachableAgents;
        agent1Json.reachableTargetIds = reachableTargets;
        return agent1Json;
    }
}
