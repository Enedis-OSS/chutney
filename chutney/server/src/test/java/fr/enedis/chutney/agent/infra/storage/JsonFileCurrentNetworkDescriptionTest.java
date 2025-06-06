/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fr.enedis.chutney.agent.domain.configure.LocalServerIdentifier;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.network.Agent;
import fr.enedis.chutney.agent.domain.network.AgentGraph;
import fr.enedis.chutney.agent.domain.network.ImmutableNetworkDescription;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;

public class JsonFileCurrentNetworkDescriptionTest {

    JsonFileCurrentNetworkDescription underTest;

    EmbeddedEnvironmentApi environmentApi = mock(EmbeddedEnvironmentApi.class);
    AgentNetworkMapperJsonFileMapper agentNetworkMapperJsonFileMapper = mock(AgentNetworkMapperJsonFileMapper.class);
    JsonFileAgentNetworkDao jsonFileAgentNetworkDao = mock(JsonFileAgentNetworkDao.class);
    LocalServerIdentifier localServerIdentifier = mock(LocalServerIdentifier.class);

    NetworkDescription originalNetworkDescription;
    Agent originalLocalAgent = mock(Agent.class);

    @BeforeEach
    public void setUp() {
        reset(environmentApi, agentNetworkMapperJsonFileMapper, jsonFileAgentNetworkDao, localServerIdentifier);

        originalNetworkDescription = createNetworkDescription();
        NetworkDescription anotherNetworkDescription = createNetworkDescription();

        when(jsonFileAgentNetworkDao.read()).thenReturn(Optional.of(mock(AgentNetworkForJsonFile.class)));
        when(agentNetworkMapperJsonFileMapper.fromDto(any(), any())).thenReturn(originalNetworkDescription).thenReturn(anotherNetworkDescription);
        when(localServerIdentifier.findLocalAgent(any())).thenReturn(originalLocalAgent);

        underTest = new JsonFileCurrentNetworkDescription(
            environmentApi,
            agentNetworkMapperJsonFileMapper,
            jsonFileAgentNetworkDao,
            localServerIdentifier);
    }

    private NetworkDescription createNetworkDescription() {
        NetworkConfiguration networkConfiguration = mock(NetworkConfiguration.class);
        AgentGraph agentGraph = mock(AgentGraph.class);
        return ImmutableNetworkDescription.builder().configuration(networkConfiguration).agentGraph(agentGraph).build();
    }
}
