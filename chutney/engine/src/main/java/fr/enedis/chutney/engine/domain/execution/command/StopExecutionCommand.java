/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.command;

import fr.enedis.chutney.engine.domain.execution.event.Event;

public class StopExecutionCommand implements Event {

    private final Long executionId;
    public final boolean force;

    /**
     * @param force true to interrupt the running step, false to let it end
     */
    public StopExecutionCommand(Long executionId, boolean force) {
        this.executionId = executionId;
        this.force = force;
    }

    @Override
    public long executionId() {
        return executionId;
    }
}
