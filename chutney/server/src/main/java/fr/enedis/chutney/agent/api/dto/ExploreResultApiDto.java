/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * DTO for ExploreResult transport.
 */
public class ExploreResultApiDto {

    public Set<AgentLinkEntity> agentLinks = new LinkedHashSet<>();

    public final Set<TargetLinkEntity> targetLinks = new LinkedHashSet<>();

    public static class AgentLinkEntity {
        public String source;
        public String destination;

        public AgentLinkEntity() {
        }

        public AgentLinkEntity(String source, String destination) {
            this.source = source;
            this.destination = destination;
        }
    }

    public static class TargetLinkEntity {
        public final String source;
        public final TargetIdEntity destination;

        public TargetLinkEntity(String source, TargetIdEntity destination) {
            this.source = source;
            this.destination = destination;
        }
    }
}
