/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.http

import fr.enedis.chutney.demo.spec.SWAPISpecs
import fr.enedis.chutney.demo.spec.SWAPISpecs.root_list_all_resources
import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.Environment
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.Test

class SwapiTest {
    private var environment: ChutneyEnvironment = Environment(name = "demo", description = "demo environment") {
        Target {
            Name(SWAPISpecs.TARGET)
            Url("https://swapi.dev/api")
        }
    }

    @Test
    fun `root list all resources`() {
        Launcher().run(root_list_all_resources, environment)
    }
}
