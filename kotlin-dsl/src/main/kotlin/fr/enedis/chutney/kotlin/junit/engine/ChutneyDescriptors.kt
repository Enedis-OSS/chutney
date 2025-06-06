/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine

import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.ChutneyScenario
import fr.enedis.chutney.kotlin.dsl.ChutneyStep
import fr.enedis.chutney.kotlin.dsl.RetryTimeOutStrategy
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor

class ChutneyEngineDescriptor(uniqueId: UniqueId, displayName: String) : EngineDescriptor(uniqueId, displayName)

class ChutneyClassDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    source: TestSource
) : AbstractTestDescriptor(uniqueId, displayName, source) {

    override fun getType(): TestDescriptor.Type {
        return TestDescriptor.Type.CONTAINER
    }
}

class ChutneyScenarioDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    source: TestSource,
    val chutneyScenario: ChutneyScenario,
    val environmentName: String,
    val environment: ChutneyEnvironment?,
    private val stepAsTest: Boolean
) : AbstractTestDescriptor(uniqueId, displayName, source) {

    override fun getType(): TestDescriptor.Type {
        return if (stepAsTest) TestDescriptor.Type.CONTAINER_AND_TEST else TestDescriptor.Type.TEST
    }
}

class ChutneyStepDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    source: TestSource,
    private val chutneyStep: ChutneyStep
) : AbstractTestDescriptor(uniqueId, displayName, source) {

    override fun getType(): TestDescriptor.Type {
        return chutneyStep.implementation?.let { TestDescriptor.Type.TEST }
            ?: TestDescriptor.Type.CONTAINER_AND_TEST
    }

    fun hasRetryStrategy(): Boolean {
        return RetryTimeOutStrategy.TYPE == chutneyStep.strategy?.type
    }
}
