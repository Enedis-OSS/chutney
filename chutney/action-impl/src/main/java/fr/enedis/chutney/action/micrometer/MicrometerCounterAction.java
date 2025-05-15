/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.micrometer;

import static fr.enedis.chutney.action.micrometer.MicrometerActionHelper.doubleStringValidation;
import static fr.enedis.chutney.action.micrometer.MicrometerActionHelper.parseDoubleOrNull;
import static fr.enedis.chutney.action.micrometer.MicrometerActionHelper.toOutputs;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;
import static io.micrometer.core.instrument.Metrics.globalRegistry;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.validation.Validator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;

public class MicrometerCounterAction implements Action {

    protected static final String OUTPUT_COUNTER = "micrometerCounter";

    private final Logger logger;
    private final String name;
    private final String description;
    private final String unit;
    private final List<String> tags;
    private Counter counter;
    private final MeterRegistry registry;
    private final String increment;

    public MicrometerCounterAction(Logger logger,
                                 @Input("name") String name,
                                 @Input("description") String description,
                                 @Input("unit") String unit,
                                 @Input("tags") List<String> tags,
                                 @Input("counter") Counter counter,
                                 @Input("registry") MeterRegistry registry,
                                 @Input("increment") String increment) {
        this.logger = logger;
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.tags = tags;
        this.increment = increment;
        this.counter = counter;
        this.registry = ofNullable(registry).orElse(globalRegistry);
    }

    @Override
    public List<String> validateInputs() {
        Validator<Object> metricNameValidation = of(null)
            .validate(a -> name != null || counter != null, "name and counter cannot be both null");

        return getErrorsFrom(
            metricNameValidation,
            doubleStringValidation(increment, "increment")
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            this.counter = ofNullable(counter).orElseGet(() -> this.retrieveCounter(registry));
            if (increment != null) {
                counter.increment(parseDoubleOrNull(increment));
                logger.info("Counter incremented by " + increment);
            }
            logger.info("Counter current count is " + counter.count());
            return ActionExecutionResult.ok(toOutputs(OUTPUT_COUNTER, counter));
        } catch (Exception e) {
            logger.error(e);
            return ActionExecutionResult.ko();
        }
    }

    private Counter retrieveCounter(MeterRegistry registry) {

        Counter.Builder builder = Counter.builder(requireNonNull(name))
            .description(description)
            .baseUnit(unit);

        ofNullable(tags).ifPresent(t -> builder.tags(t.toArray(new String[0])));

        return builder.register(registry);
    }
}
