/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain.exception;

public class AlreadyExistingTargetException extends RuntimeException {

    public AlreadyExistingTargetException(String targetName, String environmentName) {
        super("Target [" + targetName + "] already exists in [" + environmentName + "] environment");
    }

}
