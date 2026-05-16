/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.target.dto;

import static fr.enedis.chutney.tools.Entry.toEntrySet;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.enedis.chutney.environment.domain.Target;
import fr.enedis.chutney.tools.Entry;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetDto {
    public final String name;
    public final String url;
    public final String environment;
    public final Set<Entry> properties;

    @JsonCreator
    public TargetDto(
        @JsonProperty("name") String name,
        @JsonProperty("url") String url,
        @JsonProperty("environment") String environment,
        @JsonProperty("properties") Set<Entry> properties
    ) {
        this.name = trimIfNotNull(name);
        this.url = trimIfNotNull(url);
        this.environment = trimIfNotNull(environment);
        this.properties = nullToEmpty(properties);
    }

    public TargetDto(String name, String url, Set<Entry> properties) {
        this(name, url, null, properties);
    }

    public Target toTarget(String environment) {
        return Target.builder()
            .withName(name)
            .withEnvironment(environment)
            .withUrl(url)
            .withProperties(propertiesToMap())
            .build();
    }

    public Target toTarget() {
        return Target.builder()
            .withName(name)
            .withEnvironment(environment)
            .withUrl(url)
            .withProperties(propertiesToMap())
            .build();
    }

    public TargetDto copyWithEnvironment(String environment) {
        return new TargetDto(this.name, this.url, environment, this.properties);
    }

    public static TargetDto from(Target target) {
        return new TargetDto(
            target.name,
            target.url,
            target.environment,
            toEntrySet(target.properties)
        );
    }

    public Map<String, String> propertiesToMap() {
        return properties == null ? emptyMap() : properties.stream().collect(Collectors.toMap(p -> p.key, p -> p.value));
    }

    private <T> Set<T> nullToEmpty(Set<T> set) {
        return set == null ? emptySet() : set;
    }

    private String trimIfNotNull(String str) {
        return str != null ? str.trim() : null;
    }
}
