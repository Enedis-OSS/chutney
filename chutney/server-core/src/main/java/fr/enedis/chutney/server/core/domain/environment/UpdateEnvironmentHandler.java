/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.environment;

public interface UpdateEnvironmentHandler {

    void renameEnvironment(String oldName, String newName);
    void deleteEnvironment(String environmentName);
}
