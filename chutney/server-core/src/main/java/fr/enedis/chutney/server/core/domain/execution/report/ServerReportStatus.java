/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution.report;

import com.google.common.collect.Ordering;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public enum ServerReportStatus {
    SUCCESS, WARN, FAILURE, NOT_EXECUTED, STOPPED, PAUSED, RUNNING, SKIPPED;

    private static final Ordering<ServerReportStatus> EXECUTION_STATUS_STATUS_ORDERING = Ordering.explicit(PAUSED, RUNNING, STOPPED, FAILURE, WARN, NOT_EXECUTED, SUCCESS, SKIPPED);

    public static ServerReportStatus worst(Iterable<ServerReportStatus> severalStatus) {
        List<ServerReportStatus> nonNullStatus = StreamSupport
            .stream(severalStatus.spliterator(), false)
            .filter(Objects::nonNull)
            .toList();

        List<ServerReportStatus> executedStatus = nonNullStatus.stream().filter(s -> !s.equals(SKIPPED)).toList();
        if (executedStatus.isEmpty()) {
            return nonNullStatus.isEmpty() ? SUCCESS : SKIPPED;
        }

        return executedStatus.stream()
            .reduce(SUCCESS, EXECUTION_STATUS_STATUS_ORDERING::min);
    }

    public boolean isFinal() {
        return this.equals(FAILURE) || this.equals(SUCCESS) || this.equals(STOPPED) || this.equals(SKIPPED);
    }

    public interface HavingStatus {
        ServerReportStatus getStatus();
    }
}
