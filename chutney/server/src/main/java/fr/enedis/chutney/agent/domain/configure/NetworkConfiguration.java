/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.configure;

import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.domain.Environment;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;
import org.immutables.value.Value;

/**
 * Network global configuration.
 */
@Value.Immutable
@Value.Enclosing
public interface NetworkConfiguration {

    /**
     * Used to determine if a configuration have already been applied,
     * and to avoid infinite-loop / dead-lock when propagating the configuration in the agent network.
     */
    Instant creationDate();

    /**
     * Set of {@link NamedHostAndPort} representing the agent network.
     */
    AgentNetworkConfiguration agentNetworkConfiguration();

    /**
     * Set of {@link Environment} representing environment declared.
     */
    EnvironmentConfiguration environmentConfiguration();

    @Value.Immutable
    interface AgentNetworkConfiguration {

        @Value.Parameter
        Set<NamedHostAndPort> agentInfos();

        default Stream<NamedHostAndPort> stream() {
            return agentInfos().stream();
        }
    }

    @Value.Immutable
    interface EnvironmentConfiguration {

        @Value.Parameter
        Set<EnvironmentDto> environments();

        default Stream<EnvironmentDto> stream() {
            return environments().stream();
        }
    }
}
