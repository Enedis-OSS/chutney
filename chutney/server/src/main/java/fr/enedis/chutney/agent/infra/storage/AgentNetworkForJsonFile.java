/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import java.time.Instant;
import java.util.List;

class AgentNetworkForJsonFile {
    Instant configurationCreationDate;
    List<AgentForJsonFile> agents;
}
