/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

public record ScenarioJiraLink(Long campaignId,
                               Long campaignExecutionId,
                               String scenarioId,
                               String datasetId,
                               String jiraId) {

}
