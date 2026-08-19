/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine.execution

import fr.enedis.chutney.engine.domain.execution.report.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult

@DisplayName("JUnit result from Chutney status")
class ExecutionUtilTest {

    @Test
    fun `skipped status is not a failure`() {
        val result = testExecutionResultFromStatus(null, Status.SKIPPED)

        assertThat(result.status).isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
    }

    @Test
    fun `skipped alongside a success is not a failure`() {
        val result = testExecutionResultFromStatus(null, Status.SKIPPED, Status.SUCCESS)

        assertThat(result.status).isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
    }

    @Test
    fun `skipped alongside a failure is still a failure`() {
        val result = testExecutionResultFromStatus(null, Status.SKIPPED, Status.FAILURE)

        assertThat(result.status).isEqualTo(TestExecutionResult.Status.FAILED)
    }

    @Test
    fun `success is not a failure`() {
        val result = testExecutionResultFromStatus(null, Status.SUCCESS)

        assertThat(result.status).isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
    }

    @Test
    fun `failure is a failure`() {
        val result = testExecutionResultFromStatus(null, Status.FAILURE)

        assertThat(result.status).isEqualTo(TestExecutionResult.Status.FAILED)
    }

    @Test
    fun `no status at all is not a failure`() {
        val result = testExecutionResultFromStatus(null)

        assertThat(result.status).isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
    }
}
