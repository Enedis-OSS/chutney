/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.spi.time;


import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Expected duration format: "floating_positive_number [duration_unit]" where
 * floating_positive_number : the duration value (ex.: 10)
 * time_unit : the duration unit. Valid values are:
 * <ul>
 * <li> "min" or "m" for minutes</li>
 * <li> "sec" or "s" for seconds</li>
 * <li> "ms" for milliseconds</li>
 * </ul>
 * Examples: "5 min", or "300 sec", ...
 */
public class Duration {
    private static final DecimalFormat DISPLAY_FORMATTER = new DecimalFormat("#.##");
    private static final DurationParser[] PARSERS = new DurationParser[]{
        new DurationWithUnitParser(),
        new UntilHourDurationParser()
    };
    private static final String ERROR_MESSAGE_TEMPLATE = "Cannot parse duration: %1$s\nAvailable patterns are:\n" + buildErrorMessageTemplate();

    private final double durationValue;
    private final DurationUnit durationUnit;

    Duration(double durationValue, DurationUnit durationUnit) {
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
    }

    private static String buildErrorMessageTemplate() {
        return Arrays.stream(PARSERS)
            .map(DurationParser::description)
            .map(d -> "- " + d)
            .collect(Collectors.joining("\n"));
    }

    public long toMilliseconds() {
        return Math.round(durationUnit.toMilliFactor * durationValue);
    }

    public static Duration parse(String literalDuration) {
        Optional<Duration> parsed = Arrays.stream(PARSERS)
            .map(parser -> parser.parse(literalDuration))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();

        return parsed.orElseThrow(() -> new IllegalArgumentException(String.format(ERROR_MESSAGE_TEMPLATE, literalDuration)));
    }

    public static long parseToMs(String literalDuration) {
        return parse(literalDuration).toMilliseconds();
    }

    @Override
    public String toString() {
        return DISPLAY_FORMATTER.format(durationValue).replace(',', '.') + " " + durationUnit;
    }

    @SuppressWarnings("unused")
    public double getDurationValue() {
        return durationValue;
    }

    @SuppressWarnings("unused")
    public DurationUnit getDurationUnit() {
        return durationUnit;
    }
}
