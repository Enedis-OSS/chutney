/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine

import fr.enedis.chutney.kotlin.dsl.ChutneyScenario
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.SuccessAction
import fr.enedis.chutney.kotlin.annotations.ChutneyTest

class SameMethodNameClassTest {

    @ChutneyTest
    fun sameMethodNameInOtherClassTest(): ChutneyScenario {
        return Scenario(title = "A scenario") {
            When("Something happens") {
                SuccessAction()
            }
        }
    }
}
