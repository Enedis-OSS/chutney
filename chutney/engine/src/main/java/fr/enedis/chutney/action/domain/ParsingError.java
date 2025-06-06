/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.domain;

/**
 * Error produced by a {@link ActionTemplateParser} if the parsing fails.
 */
public class ParsingError {
    private final Class<?> actionClass;
    private final String errorMessage;

    public ParsingError(Class<?> actionClass, String errorMessage) {
        this.actionClass = actionClass;
        this.errorMessage = errorMessage;
    }

    /**
     * @return the class that failed to be parsed
     */
    public Class<?> actionClass() {
        return actionClass;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
