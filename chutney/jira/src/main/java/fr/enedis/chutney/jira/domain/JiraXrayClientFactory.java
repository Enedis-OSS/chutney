/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.domain;

public interface JiraXrayClientFactory {

    JiraXrayApi create(JiraServerConfiguration jiraServerConfiguration);

}
