/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.http

import fr.enedis.chutney.example.scenario.HTTP_TARGET_NAME
import fr.enedis.chutney.kotlin.dsl.*
import fr.enedis.chutney.kotlin.launcher.Launcher
import org.junit.jupiter.api.Test

class HttpServerTest {

    @Test
    fun name() {
        val environment = Environment(name = "local", description = "local environment") {
            Target {
                Name(HTTP_TARGET_NAME)
                Url("https://localhost:8443")
            }
        }

        val scenario = Scenario(title = "mgn") {
            When("when") {
                HttpsServerStartAction("8443", null, null, null, null, null, emptyMap() ,emptyMap())
            }
            Then("then") {
                HttpGetAction(
                    target = HTTP_TARGET_NAME,
                    uri = "/service/get"
                )
            }
            And("assert ok") {
                HttpsListenerAction(
                    uri = "/.*",
                    verb = "GET",
                    expectedMessageCount = 1
                )
            }
        }

        Launcher().run(scenario, environment)
    }
}
