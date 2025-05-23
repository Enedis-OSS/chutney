/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.execution;

@SuppressWarnings("serial")
public class ScenarioConversionException extends RuntimeException {

    public ScenarioConversionException(String scenarioId, Exception e) {
        super("Unable to convert scenario [" + scenarioId + "]: " + e.getMessage(), e);
    }

    public ScenarioConversionException(String scenarioId, String message) {
        super("Unable to convert scenario [" + scenarioId + "]: " + message);
    }

    public ScenarioConversionException(Exception e) {
        super("Unable to convert scenario: " + e.getMessage(), e);
    }
}
