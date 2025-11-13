/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain.exception;

public class UnresolvedEnvironmentException extends RuntimeException {
    public UnresolvedEnvironmentException() {
        super("There is more than one environment. Could not resolve the default one");
    }
}
