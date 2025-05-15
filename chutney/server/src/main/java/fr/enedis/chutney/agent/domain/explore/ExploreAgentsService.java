/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.explore;

import static fr.enedis.chutney.agent.domain.explore.ImmutableExploreResult.Links.of;

import fr.enedis.chutney.agent.domain.AgentClient;
import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.agent.domain.configure.ConfigurationState;
import fr.enedis.chutney.agent.domain.configure.Explorations;
import fr.enedis.chutney.agent.domain.configure.LocalServerIdentifier;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.explore.ExploreResult.Links;
import fr.enedis.chutney.agent.domain.explore.ImmutableExploreResult.Link;
import fr.enedis.chutney.agent.domain.network.NetworkDescription;
import fr.enedis.chutney.engine.domain.delegation.ConnectionChecker;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to explore agents from the current one, given a {@link NetworkConfiguration}.<br>
 * Exploration performs two things :
 * <ul>
 * <li>propagate a {@link NetworkConfiguration} recursively to all reachable agents</li>
 * <li>aggregate discovered agentLinks between agents</li>
 * </ul>
 */
public class ExploreAgentsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreAgentsService.class);

    private final Explorations explorations;
    private final AgentClient agentClient;

    private final ConnectionChecker connectionChecker;
    private final LocalServerIdentifier localServerIdentifier;

    public ExploreAgentsService(Explorations explorations,
                                AgentClient agentClient,
                                ConnectionChecker connectionChecker,
                                LocalServerIdentifier localServerIdentifier) {
        this.explorations = explorations;
        this.agentClient = agentClient;
        this.connectionChecker = connectionChecker;
        this.localServerIdentifier = localServerIdentifier;
    }

    /**
     * Propagate given {@link NetworkConfiguration} recursively to all reachable agents.
     *
     * @return agentLinks &amp; targetLinks discovered during propagation of the given {@link NetworkConfiguration} among agents
     */
    public ExploreResult explore(NetworkConfiguration networkConfiguration) {
        if (explorations.changeStateToIfPossible(networkConfiguration, ConfigurationState.EXPLORING)) {
            return exploreAndMerge(networkConfiguration);
        } else {
            LOGGER.debug("Received already applied configure");
            return ExploreResult.EMPTY;
        }
    }

    /**
     * Propagate final {@link NetworkDescription} recursively to all reachable agents.
     */
    public void wrapUp(NetworkDescription networkDescription) {
        if (explorations.changeStateToIfPossible(networkDescription.configuration(), ConfigurationState.WRAPING_UP)) {
            dispatch(networkDescription);
            explorations.changeStateToIfPossible(networkDescription.configuration(), ConfigurationState.FINISHED);
        } else {
            LOGGER.debug("Received already applied description");
        }
    }

    private ExploreResult exploreAndMerge(NetworkConfiguration networkConfiguration) {
        String localName = localServerIdentifier.getLocalName(networkConfiguration);

        Set<ExploreResult> exploreResults = exploreToRemoteAgentsAndTargets(localName, networkConfiguration);
        Links<AgentId, AgentId> agentLinks = aggregateLinks(exploreResults, ExploreResult::agentLinks);
        Links<AgentId, TargetId> targetLinks = aggregateLinks(exploreResults, ExploreResult::targetLinks);

        return ImmutableExploreResult.of(agentLinks, targetLinks);
    }

    private ExploreResult detectAvailableTargets(String localName, NetworkConfiguration networkConfiguration) {
        Set<Link<AgentId, TargetId>> targetLinks = networkConfiguration.environmentConfiguration().stream()
            .flatMap(e -> e.targets.stream()
                .map(t -> Pair.of(e.name, t))
                .filter(p -> {
                    try {
                        NamedHostAndPort namedHostAndPort = namedHostAndPortFromTarget(p.getRight());
                        return connectionChecker.canConnectTo(namedHostAndPort);
                    } catch (UndefinedPortException ex) {
                        LOGGER.warn(ex.getMessage());
                        return false;
                    }
                })
                .map(p -> TargetId.of(p.getRight().name, p.getLeft()))
                .map(targetIdentifier -> Link.of(AgentId.of(localName), targetIdentifier))
            ).collect(Collectors.toSet());
        return ImmutableExploreResult.of(Links.empty(), ImmutableExploreResult.Links.of(targetLinks));
    }

    private Set<ExploreResult> exploreToRemoteAgentsAndTargets(String localName, NetworkConfiguration networkConfiguration) {
        Set<ExploreResult> remoteExplorationResults = networkConfiguration.agentNetworkConfiguration().stream()
            .filter(agentInfo -> !localName.equals(agentInfo.name()))
            .map(agentInfo -> agentClient.explore(localName, agentInfo, networkConfiguration))
            .collect(Collectors.toSet());

        ExploreResult localTargetsExplorationResult = detectAvailableTargets(localName, networkConfiguration);

        return ImmutableSet.<ExploreResult>builder().addAll(remoteExplorationResults).add(localTargetsExplorationResult).build();
    }

    private <S, D> Links<S, D> aggregateLinks(Set<ExploreResult> exploreResults, Function<ExploreResult, Links<S, D>> linksExtractor) {
        return of(exploreResults.stream()
            .map(linksExtractor)
            .flatMap(Links::stream)
            .collect(Collectors.toSet()));
    }

    private void dispatch(NetworkDescription networkDescription) {
        String localName = localServerIdentifier.getLocalName(networkDescription.configuration());

        networkDescription.configuration().agentNetworkConfiguration().stream()
            .filter(agentInfo -> !localName.equals(agentInfo.name()))
            .forEach(agentInfo -> agentClient.wrapUp(agentInfo, networkDescription));
    }

    private NamedHostAndPort namedHostAndPortFromTarget(TargetDto target) {
        UrlSlicer urlSlicer = new UrlSlicer(target.url);
        return new NamedHostAndPort(target.name, urlSlicer.host, urlSlicer.port);
    }
}
