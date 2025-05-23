/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.tests.actions

import fr.enedis.chutney.acceptance.common.*
import fr.enedis.chutney.kotlin.dsl.*


fun `Jakarta actionInputs wrong url`(
  jakartaAction: String,
  actionInputs: String
): ChutneyScenario {
  return Scenario(title = "Jakarta actionInputs wrong url") {
    val environmentName = "JAK_$jakartaAction"
    Given("A target pointing to an unknown service") {
      createEnvironment(
        environmentName,
        """
        [
          {
              "name": "test_jakarta",
              "url": "tcp://$UNKNOWN_TARGET",
              "properties": [
                  {
                      "key": "java.naming.factory.initial",
                      "value": "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
                  }
              ]
          }
        ]
      """.trimIndent()
      )
    }
    And("this scenario is saved") {
      createScenario(
        "scenarioId",
        """
        {
            "when":{
                "sentence":"Make failed jakarta request",
                "implementation":{
                    "task":"{\n type: jakarta-$jakartaAction \n target: test_jakarta \n inputs: {\n $actionInputs \n} \n}"
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
      executeScenario("scenarioId".spEL, environmentName)
    }
    Then("the report status is FAILURE") {
      checkScenarioReportFailure()
    }
  }
}

fun `Jakarta sender then clean then send and listen it on embedded broker`(
  port: Int
): ChutneyScenario {

  return Scenario(title = "Jakarta sender then clean then send and listen it on embedded broker") {
    Given("a jakarta endpoint") {
      JakartaBrokerStartAction(
        configUri ="tcp://0.0.0.0:$port"
      )
    }
    And("An associated target") {
      createEnvironment(
        "JAKARTA_ENV_OK",
        """
        [
          {
              "name": "test_jakarta",
              "url": "tcp://host.testcontainers.internal:$port",
              "properties": [
                  {
                      "key": "java.naming.factory.initial",
                      "value": "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
                  }
              ]
          }
        ]
      """.trimIndent()
      )
    }
    And("This scenario is saved") {
      createScenario(
        "scenarioId",
        """
        {
            "when":{
                "sentence":"Send jakarta Message",
                "implementation":{
                    "task":"{\n type: jakarta-sender \n target: test_jakarta \n inputs: {\n destination: dynamicQueues/test \n body: something \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Clean queue",
                    "implementation":{
                        "task":"{\n type: jakarta-clean-queue \n target: test_jakarta \n inputs: {\n destination: dynamicQueues/test \n} \n}"
                    }
                },
                {
                    "sentence":"Send jakarta Message",
                    "implementation":{
                        "task":"{\n type: jakarta-sender \n target: test_jakarta \n inputs: {\n destination: dynamicQueues/test \n body: message to catch \n} \n}"
                    }
                },
                {
                    "sentence":"Listen to queue",
                    "implementation":{
                        "task":"{\n type: jakarta-listener \n target: test_jakarta \n inputs: {\n destination: dynamicQueues/test \n} \n}"
                    }
                },
                {
                    "sentence":"Check jakarta message",
                    "implementation":{
                        "task":"{\n type: string-assert \n inputs: {\n document: ${"textMessage".hjsonSpEL} \n expected: message to catch \n} \n}"
                    }
                }
            ]
        }
        """.trimIndent()
      )
    }
    When("The scenario is executed") {
      executeScenario("scenarioId".spEL, "JAKARTA_ENV_OK")
    }
    Then("the report status is SUCCESS") {
      checkScenarioReportSuccess()
    }
  }
}