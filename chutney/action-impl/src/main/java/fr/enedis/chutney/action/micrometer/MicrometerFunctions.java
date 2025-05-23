/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.micrometer;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

import fr.enedis.chutney.action.spi.SpelFunction;
import io.micrometer.core.instrument.MeterRegistry;

public class MicrometerFunctions {

    @SpelFunction
    public static MeterRegistry micrometerRegistry(String registryClassName) {
        if (registryClassName == null || registryClassName.isBlank()) {
            return globalRegistry;
        }

        return globalRegistry.getRegistries().stream()
            .filter(mr -> mr.getClass().getSimpleName().contains(registryClassName))
            .findFirst()
            .orElse(globalRegistry);
    }
}
