/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.example.http

import com.chutneytesting.demo.spec.SWAPISpecs
import com.chutneytesting.demo.spec.SWAPISpecs.root_list_all_resources
import com.chutneytesting.kotlin.dsl.ChutneyEnvironment
import com.chutneytesting.kotlin.dsl.Environment
import com.chutneytesting.kotlin.launcher.Launcher
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
