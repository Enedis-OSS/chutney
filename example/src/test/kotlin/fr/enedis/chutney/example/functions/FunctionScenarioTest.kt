/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.functions

import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.Test

class FunctionScenarioTest {

    private var environment: ChutneyEnvironment = ChutneyEnvironment("default value")

    @Test
    fun `jsonMerge` () {
        Launcher().run(json_merge_scenario, environment)
    }

    @Test
    fun `jsonSet` () {
        Launcher().run(json_set_scenario, environment)
    }

    @Test
    fun `jsonSetMany` () {
        Launcher().run(json_set_many_scenario, environment)
    }
}
