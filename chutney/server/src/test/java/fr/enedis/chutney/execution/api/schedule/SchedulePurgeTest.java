/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.api.schedule;

import static java.lang.Thread.sleep;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import fr.enedis.chutney.server.core.domain.execution.history.PurgeService;
import fr.enedis.chutney.server.core.domain.execution.history.PurgeService.PurgeReport;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class SchedulePurgeTest {

    @Test
    void return_purge_report() {
        PurgeService mockPurge = mock(PurgeService.class);
        when(mockPurge.purge()).thenReturn(new PurgeReport(Set.of(1L, 2L), Set.of(3L, 4L)));
        SchedulePurge sut = new SchedulePurge(mockPurge, 2, 0);
        assertThat(sut.launchPurge()).hasValue(new PurgeReport(Set.of(1L, 2L), Set.of(3L, 4L)));
        verify(mockPurge).purge();
    }

    @Nested
    @DisplayName("Fails to purge on exception")
    class WithFailedPurge {

        private static Level sutLoggerLevel;

        @BeforeAll
        public static void beforeAll() {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger(SchedulePurge.class.getName());
            sutLoggerLevel = logger.getLevel();
            logger.setLevel(Level.OFF);
        }

        @AfterAll
        public static void afterAll() {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger(SchedulePurge.class.getName());
            logger.setLevel(sutLoggerLevel);
        }

        @Test
        void handle_runtime_exceptions() {
            PurgeService mockPurge = mock(PurgeService.class);
            when(mockPurge.purge()).thenThrow(new RuntimeException("Purge failed !!"));
            SchedulePurge sut = new SchedulePurge(mockPurge, 1, 0);
            assertThat(sut.launchPurge()).isEmpty();
            verify(mockPurge).purge();
        }

        @Test
        void timeout_on_long_purge() {
            PurgeService mockPurge = mock(PurgeService.class);
            when(mockPurge.purge()).thenAnswer(invocation -> {
                sleep(18000);
                return new PurgeReport(emptySet(), emptySet());
            });
            SchedulePurge sut = new SchedulePurge(mockPurge, 2, 0);
            assertThat(sut.launchPurge()).isEmpty();
            verify(mockPurge).purge();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3})
        void retry_on_purge_failure(int retryCount) {
            PurgeService mockPurge = mock(PurgeService.class);
            when(mockPurge.purge()).thenThrow(new RuntimeException("Purge failed !!"));
            SchedulePurge sut = new SchedulePurge(mockPurge, 1, retryCount);
            assertThat(sut.launchPurge()).isEmpty();
            verify(mockPurge, times(retryCount + 1)).purge();
        }
    }
}
