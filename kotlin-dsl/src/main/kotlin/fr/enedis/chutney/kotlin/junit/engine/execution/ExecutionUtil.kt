/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine.execution

import fr.enedis.chutney.engine.domain.execution.engine.step.Step
import fr.enedis.chutney.engine.domain.execution.report.Status
import org.junit.platform.engine.TestExecutionResult

open class NoStackTraceAssertionError(message: String) : AssertionError(message) {
    override fun fillInStackTrace(): Throwable {
        return this
    }
}

class StepExecutionFailedException(step: Step) :
    NoStackTraceAssertionError(
        "Step [${step.definition().name}] execution failed : \n${
            step.errors().joinToString("\n")
        }"
    )

class UnresolvedScenarioEnvironmentException(
    throwable: Throwable,
    environmentName: String? = null
) : NoStackTraceAssertionError(
    environmentName?.let { "${throwable.message}: Environment [$it] not found." }
        ?: "${throwable.message}: Please, specify a name or declare only one environment."
)

fun Step.findSubStepPath(toBeFound: Step): List<Step> {
    if (this == toBeFound) {
        return listOf(this)
    }

    this.subSteps().forEach {
        if (it == toBeFound) {
            return listOf(this, it)
        }

        val subStepPath = it.findSubStepPath(toBeFound)
        if (subStepPath.isNotEmpty()) {
            return listOf(listOf(this), subStepPath).flatten()
        }
    }

    return emptyList()
}

fun Step.isExecutionFailed(): Boolean {
    return Status.FAILURE == this.status()
}

fun testExecutionResultFromStatus(throwable: Throwable? = null, vararg status: Status): TestExecutionResult {
    if (status.isEmpty()) {
        return TestExecutionResult.successful()
    }

    val worst = Status.worst(status.asList())
    return if (Status.SUCCESS == worst) {
        TestExecutionResult.successful()
    } else {
        TestExecutionResult.failed(throwable)
    }
}
