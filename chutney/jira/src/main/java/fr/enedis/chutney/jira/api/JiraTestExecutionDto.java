/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableJiraTestExecutionDto.class)
@Value.Style(jdkOnly = true)
public interface JiraTestExecutionDto {

    @JsonProperty("id")
    String id();

    @JsonProperty("jiraScenarios")
    List<JiraDto> jiraScenarios();

    @JsonCreator
    static JiraTestExecutionDto of(
        @JsonProperty("id") String id,
        @JsonProperty("jiraScenarios") List<JiraDto> jiraScenarios
    ) {
        return ImmutableJiraTestExecutionDto.builder()
            .id(id)
            .addAllJiraScenarios(jiraScenarios)
            .build();
    }
}
