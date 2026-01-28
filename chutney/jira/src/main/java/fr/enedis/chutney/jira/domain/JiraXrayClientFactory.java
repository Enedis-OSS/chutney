/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.domain;

import fr.enedis.chutney.server.core.domain.instrument.ChutneyMetrics;

public interface JiraXrayClientFactory {

    JiraXrayApi create(JiraServerConfiguration jiraServerConfiguration, ChutneyMetrics metrics);

}
