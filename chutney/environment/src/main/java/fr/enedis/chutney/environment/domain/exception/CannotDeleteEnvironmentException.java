/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.domain.exception;

import java.io.IOException;
import java.nio.file.Path;

public class CannotDeleteEnvironmentException extends RuntimeException {

    public CannotDeleteEnvironmentException(Path environmentPath, IOException e) {
        super("Cannot delete configuration file: " + environmentPath, e);
    }
}
