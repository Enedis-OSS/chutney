/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution.report;

import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.FAILURE;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.NOT_EXECUTED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.SKIPPED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.STOPPED;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.SUCCESS;
import static fr.enedis.chutney.server.core.domain.execution.report.ServerReportStatus.WARN;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServerReportStatusTest {

    @Test
    void worst_should_be_success_when_no_status() {
        assertThat(ServerReportStatus.worst(emptyList())).isEqualTo(SUCCESS);
    }

    @Test
    void worst_should_be_skipped_when_all_skipped() {
        assertThat(ServerReportStatus.worst(List.of(SKIPPED))).isEqualTo(SKIPPED);
        assertThat(ServerReportStatus.worst(List.of(SKIPPED, SKIPPED))).isEqualTo(SKIPPED);
    }

    static Stream<Arguments> skippedMixedCases() {
        return Stream.of(
            Arguments.of(SUCCESS, List.of(SKIPPED, SUCCESS)),
            Arguments.of(FAILURE, List.of(SKIPPED, FAILURE)),
            Arguments.of(WARN, List.of(SKIPPED, WARN)),
            Arguments.of(STOPPED, List.of(SKIPPED, STOPPED)),
            Arguments.of(NOT_EXECUTED, List.of(SKIPPED, NOT_EXECUTED))
        );
    }

    @ParameterizedTest(name = "{1} -> {0}")
    @MethodSource("skippedMixedCases")
    void worst_should_ignore_skipped_when_mixed_with_executed_status(ServerReportStatus expected, List<ServerReportStatus> input) {
        assertThat(ServerReportStatus.worst(input)).isEqualTo(expected);
    }

    @Test
    void skipped_should_be_final() {
        assertThat(SKIPPED.isFinal()).isTrue();
    }
}
