/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.tests.actions

import fr.enedis.chutney.acceptance.common.*
import fr.enedis.chutney.kotlin.dsl.*

fun `Http (verb) request wrong url`(
  verb: String,
  actionInputs: String
): ChutneyScenario {

  return Scenario(title = "Http $verb request wrong url") {
    val environmentName = "HTTP_${verb}_KO"
    val verbMinorCase = verb.lowercase()
    Given("A target pointing to an unknown http server") {
      createEnvironment(
        environmentName,
        """
        [
            {
                "name": "test_http",
                "url": "http://$UNKNOWN_TARGET"
            }
        ]
        """.trimIndent()
      )
    }
    And("this scenario is saved") {
      createScenario("scenarioId",
        """
        {
            "when":{
                "sentence":"Make failed <verb> request",
                "implementation":{
                    "task":"{\n type: http-$verbMinorCase \n target: test_http \n inputs: {\n $actionInputs \n timeout: 500 ms \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Assert http status",
                    "implementation":{
                        "task":"{\n type: compare \n inputs: {\n actual: ${"status".hjsonSpEL} \n expected: 200 \n mode: not equals \n} \n}"
                    }
                }
            ]
        }  
        """.trimIndent()
      )
    }
    When("The scenario is executed") {
      executeScenario("scenarioId".spEL,environmentName)
    }
    Then("the report status is FAILURE") {
      checkScenarioReportFailure()
    }
  }
}

fun `Http (verb) request local valid endpoint`(
  verb: String,
  actionInputs: String,
  port: Int
): ChutneyScenario {

  return Scenario(title = "Http $verb request local valid endpoint") {
    val environmentName = "HTTP_${verb}_OK"
    val verbMinorCase = verb.lowercase()
    Given("an app providing an http interface") {
      HttpsServerStartAction(
        port = port.toString(),
        trustStorePath = "resourcePath('blackbox/keystores/truststore.jks')".spEL,
        trustStorePassword = "truststore",
        outputs = mapOf(
          "appServer" to "httpsServer".spEL
        )
      )
    }
    And("A configured target for an endpoint") {
      createEnvironment(
        environmentName,
        """
        [
            {
                "name": "test_http",
                "url": "https://host.testcontainers.internal:$port",
                "properties": [
                    { "key" : "keyStore", "value": "/config/keystores/client.jks" },
                    { "key" : "keyStorePassword", "value": "client" },
                    { "key" : "keyPassword", "value": "client" }
                ]
            }
        ]
      """.trimIndent()
      )
    }
    And("This scenario is saved") {
      createScenario("scenarioId",
        """
        {
            "when":{
                "sentence":"Make <verb> request",
                "implementation":{
                    "task":"{\n type: http-$verbMinorCase \n target: test_http \n inputs: {\n $actionInputs \n timeout: 5000 ms \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Assert http status",
                    "implementation":{
                        "task":"{\n type: compare \n inputs: {\n actual: ${"status".hjsonSpEL} \n expected: '200' \n mode: equals \n} \n}"
                    }
                }
            ]
        }  
        """.trimIndent()
      )
    }
    When("The scenario is executed") {
      executeScenario("scenarioId".spEL,environmentName)
    }
    Then("the report status is SUCCESS") {
      checkScenarioReportSuccess()
    }
  }
}