/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.dto;

import fr.enedis.chutney.agent.domain.network.NetworkDescription;

/**
 * DTO for {@link NetworkDescription} transport.
 */
public class NetworkDescriptionApiDto {
    public AgentsGraphApiDto agentsGraph;
    public NetworkConfigurationApiDto networkConfiguration;
}
