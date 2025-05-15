/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.micrometer;

import static fr.enedis.chutney.action.micrometer.MicrometerActionTestHelper.assertSuccessAndOutputObjectType;
import static fr.enedis.chutney.action.micrometer.MicrometerTimerStartAction.OUTPUT_TIMER_SAMPLE;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

public class MicrometerTimerStartActionTest {

    private MicrometerTimerStartAction sut;

    @Test
    public void should_start_a_timing_sample() {
        // Given
        sut = new MicrometerTimerStartAction(new TestLogger(), null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndSampleObjectType(result);

        Timer.Sample timerSampleOutput = (Timer.Sample) result.outputs.get(OUTPUT_TIMER_SAMPLE);
        assertThat(timerSampleOutput).isNotNull();
    }

    @Test
    public void should_start_a_timing_sample_with_a_given_registry() {
        // Given
        MeterRegistry givenMeterRegistry = new SimpleMeterRegistry();
        sut = new MicrometerTimerStartAction(new TestLogger(), givenMeterRegistry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndSampleObjectType(result);

        Timer.Sample timerSampleOutput = (Timer.Sample) result.outputs.get(OUTPUT_TIMER_SAMPLE);
        assertThat(givenMeterRegistry.getMeters()).isEmpty();
        assertThat(timerSampleOutput).isNotNull();
    }

    @Test
    public void should_log_timing_sample_start() {
        // Given
        TestLogger logger = new TestLogger();
        sut = new MicrometerTimerStartAction(logger, null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndSampleObjectType(result);

        assertThat(logger.info).hasSize(1);
        assertThat(logger.info.getFirst()).contains("started");
    }

    private void assertSuccessAndSampleObjectType(ActionExecutionResult result) {
        assertSuccessAndOutputObjectType(result, OUTPUT_TIMER_SAMPLE, Timer.Sample.class);
    }
}
