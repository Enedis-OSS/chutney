/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.domain.exception;

public class NoJiraConfigurationException extends RuntimeException {

    public NoJiraConfigurationException() {
        super("Cannot request xray server, jira url is undefined");
    }

}
