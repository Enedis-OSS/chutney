/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain.exception;

@SuppressWarnings("serial")
public class InvalidEnvironmentNameException extends RuntimeException {
    public InvalidEnvironmentNameException() {
        super("Environment name must be of 3 to 20 letters, digits, underscore or hyphen. NOTE: Environment are stored in files, names must be of the form [A-Z0-9_\\-]{3,20}");
    }
}
