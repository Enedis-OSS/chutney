/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.environment.domain.exception;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("serial")
public class EnvironmentNotFoundException extends RuntimeException {
    public EnvironmentNotFoundException(Path environmentPath) {
        super("Configuration file not found: " + environmentPath);
    }

    public EnvironmentNotFoundException(List<String> environmentNames) {
        super("Environment not found for name " + environmentNames);
    }
}
