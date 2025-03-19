/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.engine.domain.report;

public class CannotGenerateReportException extends RuntimeException {

    public CannotGenerateReportException(String errorMessage) {
        super(errorMessage);
    }

    public CannotGenerateReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
