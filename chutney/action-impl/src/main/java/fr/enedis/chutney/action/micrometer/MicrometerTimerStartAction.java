/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.micrometer;

import static fr.enedis.chutney.action.micrometer.MicrometerActionHelper.toOutputs;
import static io.micrometer.core.instrument.Metrics.globalRegistry;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class MicrometerTimerStartAction implements Action {

    protected static final String OUTPUT_TIMER_SAMPLE = "micrometerTimerSample";

    private final Logger logger;
    private final MeterRegistry registry;

    public MicrometerTimerStartAction(Logger logger,
                                    @Input("registry") MeterRegistry registry) {
        this.logger = logger;
        this.registry = ofNullable(registry).orElse(globalRegistry);
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            Timer.Sample sample = Timer.start(registry);
            logger.info("Timing sample started");
            return ActionExecutionResult.ok(toOutputs(OUTPUT_TIMER_SAMPLE, sample));
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }
}
