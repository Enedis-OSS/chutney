/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.api.execution;

import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TargetExecutionDto {

    public final String id;
    public final String url;
    public final Map<String, String> properties;
    public final String name;
    public final List<NamedHostAndPort> agents;

    public TargetExecutionDto(String id, String url, Map<String, String> properties, List<NamedHostAndPort> agents) {
        this.id = id;
        this.name = id;
        this.url = url;
        this.properties = properties;
        this.agents = agents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetExecutionDto targetDto = (TargetExecutionDto) o;
        return id.equals(targetDto.id) &&
            url.equals(targetDto.url) &&
            properties.equals(targetDto.properties) &&
            agents.equals(targetDto.agents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, properties, agents);
    }

    @Override
    public String toString() {
        return "TargetDto{" +
            "id='" + id + '\'' +
            ", url='" + url + '\'' +
            ", properties=" + properties +
            ", agents=" + agents +
            '}';
    }
}
