/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.infra;

import fr.enedis.chutney.jira.domain.JiraServerConfiguration;
import fr.enedis.chutney.jira.domain.JiraXrayApi;
import fr.enedis.chutney.jira.domain.JiraXrayClientFactory;

public class JiraXrayFactoryImpl implements JiraXrayClientFactory {

    @Override
    public JiraXrayApi create(JiraServerConfiguration jiraServerConfiguration) {
        return new HttpJiraXrayImpl(jiraServerConfiguration);
    }

}
