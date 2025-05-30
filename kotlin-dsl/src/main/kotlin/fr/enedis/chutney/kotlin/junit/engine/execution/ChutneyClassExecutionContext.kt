/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine.execution

import fr.enedis.chutney.kotlin.junit.engine.ChutneyClassDescriptor
import fr.enedis.chutney.kotlin.junit.engine.ChutneyScenarioDescriptor
import org.junit.platform.engine.TestExecutionResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore

class ChutneyClassExecutionContext(
    val engineExecutionContext: ChutneyEngineExecutionContext,
    private val chutneyClassDescriptor: ChutneyClassDescriptor
) {
    private val scenarioExecutionContexts: MutableSet<ChutneyScenarioExecutionContext> = HashSet()
    private val syncExecutionSemaphore: Semaphore = Semaphore(1)
    private val endExecutionLatch: CountDownLatch = CountDownLatch(chutneyClassDescriptor.children.size)

    fun execute() {
        startExecution()
        try {
            chutneyClassDescriptor.children
                .filterIsInstance<ChutneyScenarioDescriptor>()
                .forEach {
                    executeScenario(it)
                }
        } finally {
            endExecution()
        }
    }

    fun endExecutionLatch() {
        syncExecutionSemaphore.release()
        endExecutionLatch.countDown()
    }

    private fun startExecution() {
        engineExecutionContext.notifyJUnitListener(ChutneyEngineExecutionContext.ListenerEvent.STARTED, chutneyClassDescriptor)
    }

    private fun executeScenario(scenarioDescriptor: ChutneyScenarioDescriptor) {
        syncExecutionSemaphore.acquire()

        val chutneyScenarioExecutionContext = ChutneyScenarioExecutionContext(this, scenarioDescriptor)
        scenarioExecutionContexts.add(chutneyScenarioExecutionContext)
        chutneyScenarioExecutionContext.execute()
    }

    private fun endExecution() {
        endExecutionLatch.await()

        try {
            engineExecutionContext.notifyJUnitListener(ChutneyEngineExecutionContext.ListenerEvent.FINISHED, chutneyClassDescriptor, TestExecutionResult.successful())
        } finally {
            engineExecutionContext.endExecutionLatch()
        }
    }
}
