/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.report;

import com.google.common.collect.Ordering;
import java.util.List;
import java.util.Objects;

public enum Status {
    SUCCESS, WARN, FAILURE, NOT_EXECUTED, STOPPED, PAUSED, RUNNING, EXECUTED, SKIPPED;

    private static final Ordering<Status> EXECUTION_STATUS_STATUS_ORDERING = Ordering.explicit(EXECUTED, PAUSED, RUNNING, STOPPED, FAILURE, WARN, NOT_EXECUTED, SUCCESS, SKIPPED);

    public static Status worst(List<Status> severalStatus) {

        List<Status> nonNullStatus = severalStatus.stream()
            .filter(Objects::nonNull)
            .toList();

        // A skipped step never ran, so it must not degrade its siblings
        List<Status> executedStatus = nonNullStatus.stream().filter(s -> !s.equals(SKIPPED)).toList();
        if (executedStatus.isEmpty()) {
            return nonNullStatus.isEmpty() ? SUCCESS : SKIPPED;
        }

        Status reducedStatus = executedStatus.stream()
            .reduce(SUCCESS, EXECUTION_STATUS_STATUS_ORDERING::min);

        if(reducedStatus.equals(Status.NOT_EXECUTED)) {
            List<Status> notExecutedStatus = nonNullStatus.stream().filter(s -> !s.equals(NOT_EXECUTED)).toList();
            if(!notExecutedStatus.isEmpty()) {
                return RUNNING;
            }
        }
        return reducedStatus;
    }

    public interface HavingStatus {
        Status getStatus();
    }
}
