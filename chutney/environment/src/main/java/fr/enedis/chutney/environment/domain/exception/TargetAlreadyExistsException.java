/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain.exception;

public class TargetAlreadyExistsException extends RuntimeException {
    public TargetAlreadyExistsException(String message) {
        super(message);
    }
}
