/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableJiraScenarioLinksDto.class)
@JsonDeserialize(as = ImmutableJiraScenarioLinksDto.class)
@Value.Style(jdkOnly = true)
public interface JiraScenarioLinksDto {

    @Nullable
    String id();

    String chutneyId();

    @Value.Default
    default Map<String, String> datasetLinks() {
        return emptyMap();
    }
}
