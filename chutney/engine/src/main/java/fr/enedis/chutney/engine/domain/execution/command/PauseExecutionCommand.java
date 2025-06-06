/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.command;

import fr.enedis.chutney.engine.domain.execution.event.Event;

public class PauseExecutionCommand implements Event {

    private final Long executionId;

    public PauseExecutionCommand(Long executionId) {
        this.executionId = executionId;
    }

    @Override
    public long executionId() {
        return executionId;
    }
}
