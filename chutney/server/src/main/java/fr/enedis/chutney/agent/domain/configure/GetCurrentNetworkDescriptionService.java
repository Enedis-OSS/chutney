/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.configure;

import fr.enedis.chutney.agent.domain.explore.CurrentNetworkDescription;
import fr.enedis.chutney.agent.domain.network.AgentGraph;
import fr.enedis.chutney.agent.domain.network.ImmutableNetworkDescription;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import java.time.Instant;
import java.util.Collections;

public class GetCurrentNetworkDescriptionService {

    private final CurrentNetworkDescription currentNetworkDescription;
    private final NetworkDescription defaultCurrent = ImmutableNetworkDescription.builder()
        .agentGraph(new AgentGraph(Collections.emptyList()))
        .configuration(ImmutableNetworkConfiguration.builder()
            .creationDate(Instant.now())
            .agentNetworkConfiguration(ImmutableNetworkConfiguration.AgentNetworkConfiguration.of(Collections.emptySet()))
            .environmentConfiguration(ImmutableNetworkConfiguration.EnvironmentConfiguration.of(Collections.emptySet()))
            .build())
        .build();

    public GetCurrentNetworkDescriptionService(CurrentNetworkDescription currentNetworkDescription) {
        this.currentNetworkDescription = currentNetworkDescription;
    }

    public NetworkDescription getCurrentNetworkDescription() {
        return currentNetworkDescription.findCurrent()
            .orElse(defaultCurrent);
    }
}
