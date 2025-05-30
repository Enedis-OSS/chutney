/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.tests.engine

import fr.enedis.chutney.acceptance.common.checkScenarioReportSuccess
import fr.enedis.chutney.acceptance.common.createScenario
import fr.enedis.chutney.acceptance.common.executeScenario
import fr.enedis.chutney.kotlin.dsl.*

val `Retry should stop after success assertion` = Scenario(title = "Retry should stop after success assertion") {
  Given("this scenario is saved") {
    createScenario(
      "scenarioId",
      """
        {
            "when":{
                "sentence":"Set stop date",
                "implementation":{
                    "task":"{\n type: context-put \n inputs: {\n entries: {\n dateTimeFormat: ss \n secondsPlus5: ${"dateFormatter(#dateTimeFormat).format(#now().plusSeconds(5))".hjsonSpEL} \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Assertion",
                    "strategy": {
                        "type": "retry-with-timeout",
                        "parameters": {
                            "timeOut": "15 sec",
                            "retryDelay": "1 sec"
                        }
                    },
                    "subSteps":[
                        {
                            "sentence":"Set current date",
                            "implementation":{
                                "task":"{\n type: context-put \n inputs: {\n entries: {\n currentSeconds: ${"dateFormatter(#dateTimeFormat).format(#now())".hjsonSpEL} \n} \n} \n}"
                            }
                        },
                        {
                            "sentence":"Check current date get to stop date",
                            "implementation":{
                                "task":"{\n type: string-assert \n inputs: {\n document: ${"secondsPlus5".hjsonSpEL} \n expected: \${'$'}{T(java.lang.String).format('%02d', new Integer(#currentSeconds) + 1)} \n} \n}"
                            }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL, "DEFAULT")
  }
  Then("the report status is SUCCESS") {
    checkScenarioReportSuccess()
  }
}