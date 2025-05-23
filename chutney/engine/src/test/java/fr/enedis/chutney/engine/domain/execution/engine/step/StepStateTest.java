/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.step;

import static fr.enedis.chutney.tools.WaitUtils.awaitDuring;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import fr.enedis.chutney.engine.domain.execution.report.Status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class StepStateTest {

    @Test
    void should_change_state() {
        StepState stepState = new StepState();

        stepState.successOccurred();
        assertThat(stepState.status()).isEqualTo(Status.SUCCESS);

        stepState.errorOccurred();
        assertThat(stepState.status()).isEqualTo(Status.FAILURE);

        stepState.stopExecution();
        assertThat(stepState.status()).isEqualTo(Status.STOPPED);

        stepState.pauseExecution();
        assertThat(stepState.status()).isEqualTo(Status.PAUSED);

        stepState.resumeExecution();
        assertThat(stepState.status()).isEqualTo(Status.RUNNING);
    }

    @Test
    void should_handle_null_or_empty_errors() {
        StepState stepState = new StepState();

        stepState.errorOccurred(null, "");
        stepState.addErrors(null, "");
        assertThat(stepState.status()).isEqualTo(Status.FAILURE);
        assertThat(stepState.errors()).isEmpty();
    }

    @Test
    void should_handle_null_or_empty_informations() {
        StepState stepState = new StepState();

        stepState.successOccurred(null, "");
        stepState.addInformation(null, "");
        assertThat(stepState.status()).isEqualTo(Status.SUCCESS);
        assertThat(stepState.informations()).isEmpty();
    }

    @Test
    void should_manage_watch_with_idempotency() {
        StepState stepState = new StepState();

        List<Long> durations = new ArrayList<>();
        IntStream.range(1, 5).forEach(i -> {
            stepState.startWatch();
            awaitDuring(100, MILLISECONDS);
            durations.add(stepState.duration().toMillis() * i);
        });
        assertThat(durations).isSorted();
        IntStream.range(0, 3).forEach(i ->
            assertThat(durations.get(i)).isLessThan(durations.get(i + 1))
        );

        durations.clear();

        IntStream.range(1, 5).forEach(i -> {
            stepState.stopWatch();
            durations.add(stepState.duration().toMillis());
        });
        Long firstStopDuration = durations.getFirst();
        assertThat(durations).containsExactly(firstStopDuration, firstStopDuration, firstStopDuration, firstStopDuration);
    }

    @Test
    void should_manage_watch_independently_of_status() {
        StepState stepState = new StepState();
        Status initialStatus = stepState.status();

        stepState.startWatch();
        assertThat(stepState.status()).isEqualTo(initialStatus);

        stepState.stopWatch();
        assertThat(stepState.status()).isEqualTo(initialStatus);
    }

    @Test
    void should_change_status_and_clean_logs_when_reset() {
        // Given
        StepState stepState = new StepState();
        stepState.addInformation("...");
        stepState.errorOccurred("...");

        assertThat(stepState.status()).isEqualTo(Status.FAILURE);
        assertThat(stepState.informations()).isNotEmpty();
        assertThat(stepState.errors()).isNotEmpty();

        // When
        stepState.reset();

        // Then
        assertThat(stepState.status()).isEqualTo(Status.NOT_EXECUTED);
        assertThat(stepState.informations()).isEmpty();
        assertThat(stepState.errors()).isEmpty();
    }

    @Test
    void should_begin_execution() {
        StepState stepState = new StepState();

        stepState.beginExecution();
        awaitDuring(100, MILLISECONDS);

        Instant startDate = stepState.startDate();
        assertThat(stepState.status()).isEqualTo(Status.RUNNING);
        assertThat(startDate).isNotNull();
        long elapse = stepState.duration().toMillis();
        assertThat(elapse).isPositive();

        awaitDuring(100, MILLISECONDS);

        // Idempotence
        stepState.beginExecution();

        assertThat(stepState.status()).isEqualTo(Status.RUNNING);
        assertThat(stepState.startDate()).isEqualTo(startDate);
        assertThat(stepState.duration().toMillis()).isGreaterThan(elapse);
    }

    @Test
    void should_end_execution() {
        StepState stepState = new StepState();
        stepState.startWatch();
        Status initialStatus = stepState.status();
        awaitDuring(100, MILLISECONDS);

        stepState.endExecution(false);

        assertThat(stepState.status()).isEqualTo(initialStatus);
        long elapse = stepState.duration().toMillis();
        assertThat(elapse).isPositive();

        // Idempotence
        stepState.endExecution(false);

        assertThat(stepState.status()).isEqualTo(initialStatus);
        assertThat(stepState.duration().toMillis()).isEqualTo(elapse);
    }

    @Test
    void should_change_parent_step_running_status_when_end_execution() {
        StepState stepState = new StepState();
        stepState.beginExecution();
        assertThat(stepState.status()).isEqualTo(Status.RUNNING);

        stepState.endExecution(true);

        assertThat(stepState.status()).isEqualTo(Status.EXECUTED);
    }

    @Nested
    @DisplayName("Concurrent messages modifications")
    class ConcurrentMessagesModification {
        @Nested
        class Informations {
            @RepeatedTest(20)
            void add_information() {
                StepState stepState = new StepState();
                for (int i = 0; i < 10000; i++) {
                    stepState.addInformation("info " + i);
                }

                assertThatNoException().isThrownBy(() -> {
                    new Thread(() -> {
                        for (int i = 0; i < 10000; i++) {
                            stepState.addInformation("new info " + i);
                        }
                    }).start();
                    stepState.informations();
                });
            }

            @RepeatedTest(20)
            void success_occurred() {
                StepState stepState = new StepState();
                for (int i = 0; i < 10000; i++) {
                    stepState.addInformation("info " + i);
                }

                assertThatNoException().isThrownBy(() -> {
                    new Thread(() -> {
                        for (int i = 0; i < 10000; i++) {
                            stepState.successOccurred("new info " + i);
                        }
                    }).start();
                    stepState.informations();
                });
            }
        }

        @Nested
        class Errors {
            @RepeatedTest(20)
            void add_error() {
                StepState stepState = new StepState();
                for (int i = 0; i < 10000; i++) {
                    stepState.addErrors("error " + i);
                }

                assertThatNoException().isThrownBy(() -> {
                    new Thread(() -> {
                        for (int i = 0; i < 10000; i++) {
                            stepState.addErrors("new error " + i);
                        }
                    }).start();
                    stepState.errors();
                });
            }

            @RepeatedTest(20)
            void error_occurred() {
                StepState stepState = new StepState();
                for (int i = 0; i < 10000; i++) {
                    stepState.addErrors("error " + i);
                }

                assertThatNoException().isThrownBy(() -> {
                    new Thread(() -> {
                        for (int i = 0; i < 10000; i++) {
                            stepState.errorOccurred("new info " + i);
                        }
                    }).start();
                    stepState.errors();
                });
            }
        }
    }
}
