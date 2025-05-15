/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.configure;

import fr.enedis.chutney.agent.domain.explore.CurrentNetworkDescription;
import fr.enedis.chutney.agent.domain.explore.ExploreAgentsService;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.network.AgentGraph;
import fr.enedis.chutney.agent.domain.network.ImmutableNetworkDescription;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;

public class ConfigureService {

    private final ExploreAgentsService exploreAgentsService;
    private final CurrentNetworkDescription currentNetworkDescription;
    private final LocalServerIdentifier localServerIdentifier;
    private final EnvironmentApi embeddedEnvironmentApi;

    public ConfigureService(ExploreAgentsService exploreAgentsService,
                            CurrentNetworkDescription currentNetworkDescription,
                            LocalServerIdentifier localServerIdentifier,
                            EnvironmentApi embeddedEnvironmentApi) {
        this.exploreAgentsService = exploreAgentsService;
        this.currentNetworkDescription = currentNetworkDescription;
        this.localServerIdentifier = localServerIdentifier;
        this.embeddedEnvironmentApi = embeddedEnvironmentApi;
    }

    public NetworkDescription configure(NetworkConfiguration networkConfiguration) {
        networkConfiguration = localServerIdentifier.withLocalHost(networkConfiguration);
        ExploreResult exploreResult = exploreAgentsService.explore(networkConfiguration);
        NetworkDescription networkDescription = buildNetworkDescription(networkConfiguration, exploreResult);
        wrapUpConfiguration(networkDescription);
        return networkDescription;
    }

    private NetworkDescription buildNetworkDescription(NetworkConfiguration networkConfiguration, ExploreResult exploreResult) {
        return ImmutableNetworkDescription.builder()
            .configuration(networkConfiguration)
            .agentGraph(AgentGraph.of(exploreResult, networkConfiguration))
            .build();
    }

    public void wrapUpConfiguration(NetworkDescription networkDescription) {
        currentNetworkDescription.switchTo(networkDescription);
        updateEnvironment(networkDescription.configuration().environmentConfiguration());
        exploreAgentsService.wrapUp(networkDescription);
    }

    private void updateEnvironment(NetworkConfiguration.EnvironmentConfiguration environmentConfigurations) {
        environmentConfigurations.stream().forEach(env -> {
            EnvironmentDto environment = new EnvironmentDto(env.name, env.description, env.targets, env.variables);
            embeddedEnvironmentApi.createEnvironment(environment, true);
        });
    }
}
