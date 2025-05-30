/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import fr.enedis.chutney.agent.domain.TargetId;
import fr.enedis.chutney.agent.domain.configure.ImmutableNetworkConfiguration;
import fr.enedis.chutney.agent.domain.configure.NetworkConfiguration;
import fr.enedis.chutney.agent.domain.explore.AgentId;
import fr.enedis.chutney.agent.domain.explore.ExploreResult;
import fr.enedis.chutney.agent.domain.explore.ImmutableExploreResult;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AgentNetworkTestUtils {

    private static final String AGENT_INFO_REGEX = "^(?<name>[^|]*)=(?<host>.*):(?<port>.*)$";
    private static final Pattern AGENT_INFO_PATTERN = Pattern.compile(AGENT_INFO_REGEX);
    private static final String TARGET_INFO_REGEX = "^(?<env>[^|]*)\\|(?<name>[^|]*)=(?<host>.*)(:(?<port>.*))?$";
    private static final Pattern TARGET_INFO_PATTERN = Pattern.compile(TARGET_INFO_REGEX);

    private static final String NODE_LINK_REGEX = "^(?<from>.*)->(?<to>[^|]*)$";
    private static final Pattern NODE_LINK_PATTERN = Pattern.compile(NODE_LINK_REGEX);
    private static final String TARGET_LINK_REGEX = "^(?<from>.*)->(?<env>[^|]*)\\|(?<name>[^|]*)$";
    private static final Pattern TARGET_LINK_PATTERN = Pattern.compile(TARGET_LINK_REGEX);
    public static final String ENV_NAME = "env";

    /**
     * @param agentOrTargetDescription array of {@link String Strings} formatted with regular expression {@value AGENT_INFO_REGEX}
     */
    static ImmutableNetworkConfiguration.Builder networkConfigurationBuilder(Instant creationInstant, String... agentOrTargetDescription) {
        ImmutableNetworkConfiguration.Builder builder = ImmutableNetworkConfiguration.builder()
            .creationDate(creationInstant);

        List<NamedHostAndPort> agentInfos = Stream.of(agentOrTargetDescription)
            .map(AgentNetworkTestUtils::createAgentInfo)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        builder.agentNetworkConfiguration(ImmutableNetworkConfiguration.AgentNetworkConfiguration.of(agentInfos));

        List<TargetDto> targets = Stream.of(agentOrTargetDescription)
            .map(aotd -> createTarget(ENV_NAME, aotd))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        EnvironmentDto env = new EnvironmentDto(ENV_NAME, null, targets);
        builder.environmentConfiguration(ImmutableNetworkConfiguration.EnvironmentConfiguration.of(singleton(env)));

        return builder;
    }

    public static Optional<NamedHostAndPort> createAgentInfo(String s) {
        Matcher matcher = AGENT_INFO_PATTERN.matcher(s);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(new NamedHostAndPort(
            matcher.group("name"),
            matcher.group("host"),
            Integer.parseInt(matcher.group("port"))));
    }

    public static Optional<TargetDto> createTarget(String envName, String s) {
        Matcher matcher = TARGET_INFO_PATTERN.matcher(s);
        if (!matcher.find()) return Optional.empty();
        String port = Optional.ofNullable(matcher.group("port")).map(p -> ":" + p).orElse("");
        return Optional.of(
            new TargetDto(matcher.group("name"), "proto://" + matcher.group("host") + port, emptySet())
        );
    }

    /**
     * @param agentOrTargetDescription array of {@link String Strings} formatted with regular expression {@value AGENT_INFO_REGEX}
     */
    public static NetworkConfiguration createNetworkConfiguration(String... agentOrTargetDescription) {
        return networkConfigurationBuilder(Instant.now(), agentOrTargetDescription).build();
    }

    public static ExploreResult createExploreResult(String... links) {
        ImmutableExploreResult.Links.Builder<AgentId, AgentId> agentLinksBuilder = ImmutableExploreResult.Links.builder();
        ImmutableExploreResult.Links.Builder<AgentId, TargetId> targetLinksBuilder = ImmutableExploreResult.Links.builder();
        Arrays.stream(links).forEach(link -> {
            buildAgentLink(link).ifPresent(agentLinksBuilder::addLinks);
            buildTargetLink(link).ifPresent(targetLinksBuilder::addLinks);
        });
        return ImmutableExploreResult.of(agentLinksBuilder.build(), targetLinksBuilder.build());
    }

    private static Optional<ExploreResult.Link<AgentId, AgentId>> buildAgentLink(String linkDescription) {
        Matcher matcher = NODE_LINK_PATTERN.matcher(linkDescription);
        if (!matcher.find()) return Optional.empty();
        AgentId from = AgentId.of(matcher.group("from"));
        AgentId to = AgentId.of(matcher.group("to"));
        return Optional.of(ImmutableExploreResult.Link.of(from, to));
    }

    private static Optional<ExploreResult.Link<AgentId, TargetId>> buildTargetLink(String linkDescription) {
        Matcher matcher = TARGET_LINK_PATTERN.matcher(linkDescription);
        if (!matcher.find()) return Optional.empty();
        AgentId from = AgentId.of(matcher.group("from"));
        TargetId to = TargetId.of(matcher.group("name"), ENV_NAME);
        return Optional.of(ImmutableExploreResult.Link.of(from, to));
    }
}
