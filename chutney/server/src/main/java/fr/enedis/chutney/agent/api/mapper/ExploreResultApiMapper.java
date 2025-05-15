/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.api.mapper;

import fr.enedis.chutney.agent.api.dto.ExploreResultApiDto;
import fr.enedis.chutney.agent.api.dto.ExploreResultApiDto.AgentLinkEntity;
import fr.enedis.chutney.agent.api.dto.ExploreResultApiDto.TargetLinkEntity;
import fr.enedis.chutney.agent.api.dto.TargetIdEntity;
import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.agent.domain.explore.AgentId;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.explore.ImmutableExploreResult;
import fr.enedis.chutney.agent.domain.explore.ImmutableExploreResult.Link;
import fr.enedis.chutney.agent.domain.explore.ImmutableExploreResult.Links;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ExploreResultApiMapper {

    public ExploreResult fromDto(ExploreResultApiDto linksEntity, AgentLinkEntity linkToExploredAgent) {
        return ImmutableExploreResult.builder()
            .agentLinks(
                Links.<AgentId, AgentId>builder()
                    .addAllLinks(
                        linksEntity.agentLinks.stream()
                            .map(linkEntity -> Link.of(AgentId.of(linkEntity.source), AgentId.of(linkEntity.destination)))
                            .collect(Collectors.toSet())
                    ).addLinks(Link.of(AgentId.of(linkToExploredAgent.source), AgentId.of(linkToExploredAgent.destination)))
                    .build()
            )
            .targetLinks(
                Links.of(
                    linksEntity.targetLinks.stream()
                        .map(targetLinkEntity -> Link.of(AgentId.of(targetLinkEntity.source), from(targetLinkEntity)))
                        .collect(Collectors.toSet())
                )
            )
            .build();
    }

    private TargetId from(TargetLinkEntity targetLinkEntity) {
        return TargetId.of(targetLinkEntity.destination.name, targetLinkEntity.destination.environment);
    }

    public ExploreResultApiDto from(ExploreResult exploreResult) {
        ExploreResultApiDto dto = new ExploreResultApiDto();

        dto.agentLinks = exploreResult.agentLinks().stream()
            .map(link -> new AgentLinkEntity(link.source().name(), link.destination().name()))
            .collect(Collectors.toSet());

        dto.targetLinks.addAll(
            exploreResult.targetLinks().stream()
                .map(link -> new TargetLinkEntity(link.source().name(), from(link.destination())))
                .collect(Collectors.toSet())
        );

        return dto;
    }

    private TargetIdEntity from(TargetId destination) {
        return new TargetIdEntity(destination.name, destination.environment);
    }
}
