/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.kotlin.junit.engine.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnresolvedScenarioEnvironmentExceptionTest {

    @Test
    fun exceptionMessageWithEnvironmentName() {
        val throwable = Throwable("Error occurred")
        val exception = UnresolvedScenarioEnvironmentException(throwable, "test-environment")

        assertEquals("Error occurred: Environment [test-environment] not found.", exception.message)
    }

    @Test
    fun exceptionMessageWithoutEnvironmentName() {
        val throwable = Throwable("Error occurred")
        val exception = UnresolvedScenarioEnvironmentException(throwable)

        assertEquals("Error occurred: Please, specify a name or declare only one environment.", exception.message)
    }
}
