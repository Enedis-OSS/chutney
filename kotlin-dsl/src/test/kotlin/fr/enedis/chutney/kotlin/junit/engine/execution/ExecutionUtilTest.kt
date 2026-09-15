/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine.execution

import fr.enedis.chutney.engine.domain.execution.report.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult

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
}
