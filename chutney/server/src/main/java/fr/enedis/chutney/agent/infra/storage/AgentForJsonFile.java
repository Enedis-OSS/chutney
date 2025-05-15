/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.infra.storage;

import java.util.List;

class AgentForJsonFile {
    String host;
    String name;
    int port;
    List<String> reachableAgentNames;
    List<TargetForJsonFile> reachableTargetIds;
}
