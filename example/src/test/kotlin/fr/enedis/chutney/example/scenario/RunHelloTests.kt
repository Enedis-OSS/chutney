/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.scenario

import fr.enedis.chutney.example.environment_en
import fr.enedis.chutney.example.environment_fr
import fr.enedis.chutney.kotlin.annotations.ChutneyTest
import fr.enedis.chutney.kotlin.dsl.ChutneyScenario
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.Test

class RunHelloTests {

    companion object {
        @JvmField
        val en_www = environment_en
    }

    @ChutneyTest(environment = "en_www")
    fun testMethod(): ChutneyScenario {
        return search_title_scenario
    }

    @Test
    fun `search title is displayed`() {
        Launcher().run(search_title_scenario, environment_fr)
    }

}
