/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution;

public class RunningScenarioExecutionDeleteException extends RuntimeException {

    public RunningScenarioExecutionDeleteException(Long scenarioExecutionId) {
        super("Cannot delete running scenario execution [" + scenarioExecutionId + "]");
    }
}
