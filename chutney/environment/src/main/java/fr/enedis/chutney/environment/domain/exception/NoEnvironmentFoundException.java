/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain.exception;

public class NoEnvironmentFoundException extends RuntimeException {
    public NoEnvironmentFoundException() {
        super("No Environment found");
    }
}
