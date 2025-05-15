/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain;

import fr.enedis.chutney.agent.domain.configure.ConfigurationState;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;

/**
 * Used to communicate from the current local agent to a remote one.
 */
public interface AgentClient {

    /**
     * @return empty if remote agent is unreachable, otherwise, return the link <b>from local to remote</b> and all agentLinks known by the remote
     */
    ExploreResult explore(String localName, NamedHostAndPort agentInfo, NetworkConfiguration networkConfiguration);

    /**
     * Propagate final {@link NetworkDescription} to agents discovered during {@link ConfigurationState#EXPLORING} phase.
     */
    void wrapUp(NamedHostAndPort agentInfo, NetworkDescription networkDescription);

}
