/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import fr.enedis.chutney.agent.domain.configure.LocalServerIdentifier;
import fr.enedis.chutney.agent.domain.explore.CurrentNetworkDescription;
import fr.enedis.chutney.agent.domain.network.Agent;
import fr.enedis.chutney.agent.domain.network.ImmutableNetworkDescription;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.environment.api.environment.EmbeddedEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import java.io.OutputStream;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JsonFileCurrentNetworkDescription implements CurrentNetworkDescription {

    private final EnvironmentApi environmentApi;
    private final AgentNetworkMapperJsonFileMapper agentNetworkMapperJsonFileMapper;
    private final JsonFileAgentNetworkDao jsonFileAgentNetworkDao;
    private final LocalServerIdentifier localServerIdentifier;

    private Optional<NetworkDescription> networkDescription;

    public JsonFileCurrentNetworkDescription(
        EmbeddedEnvironmentApi environmentApi,
        AgentNetworkMapperJsonFileMapper agentNetworkMapperJsonFileMapper,
        JsonFileAgentNetworkDao jsonFileAgentNetworkDao, LocalServerIdentifier localServerIdentifier) {
        this.environmentApi = environmentApi;
        this.agentNetworkMapperJsonFileMapper = agentNetworkMapperJsonFileMapper;
        this.jsonFileAgentNetworkDao = jsonFileAgentNetworkDao;
        this.localServerIdentifier = localServerIdentifier;
        this.networkDescription = getNetworkDescription();
    }

    @Override
    public Optional<NetworkDescription> findCurrent() {
        networkDescription = getNetworkDescription();
        return networkDescription;
    }

    @Override
    public void switchTo(NetworkDescription networkDescription) {
        AgentNetworkForJsonFile dto = agentNetworkMapperJsonFileMapper.toDto(networkDescription);
        jsonFileAgentNetworkDao.save(dto);
    }

    @Override
    public void backup(OutputStream outputStream) {
        if (getNetworkDescription().isPresent()) {
            jsonFileAgentNetworkDao.backup(outputStream);
        }
    }

    @Override
    public String name() {
        return "agents";
    }

    private Optional<NetworkDescription> getNetworkDescription() {
        Optional<NetworkDescription> newNetworkDescription = jsonFileAgentNetworkDao.read()
            .map(dto -> agentNetworkMapperJsonFileMapper.fromDto(dto, environmentApi.listEnvironments()));

        if (newNetworkDescription.isPresent()) {
            final Agent localAgent = localServerIdentifier.findLocalAgent(newNetworkDescription.get().agentGraph());
            newNetworkDescription = Optional.of(ImmutableNetworkDescription.builder().from(newNetworkDescription.get()).localAgent(localAgent).build());
        }

        return newNetworkDescription;
    }
}
