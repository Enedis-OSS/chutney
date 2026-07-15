/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableExecutionJiraLinkDto.class)
@Value.Style(jdkOnly = true)
public interface ExecutionJiraLinkDto {

    @JsonProperty("campaignJiraId")
    String campaignJiraId();

    @JsonProperty("executionJiraId")
    String executionJiraId();

    @JsonCreator
    static ExecutionJiraLinkDto of(
        @JsonProperty("campaignJiraId") String campaignJiraId,
        @JsonProperty("executionJiraId") String executionJiraId
    ) {
        return ImmutableExecutionJiraLinkDto.builder()
            .campaignJiraId(campaignJiraId)
            .executionJiraId(executionJiraId)
            .build();
    }
}
