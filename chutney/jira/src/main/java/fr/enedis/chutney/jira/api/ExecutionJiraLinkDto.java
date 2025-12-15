/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableExecutionJiraLinkDto.class)
@JsonDeserialize(as = ImmutableExecutionJiraLinkDto.class)
@Value.Style(jdkOnly = true)
public interface ExecutionJiraLinkDto {

    String campaignJiraId();

    String executionJiraId();
}
