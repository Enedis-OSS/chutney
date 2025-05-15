/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.network;

import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface NetworkDescription {
    NetworkConfiguration configuration();

    AgentGraph agentGraph();

    // TODO why optional ?
    Optional<Agent> localAgent();
}
