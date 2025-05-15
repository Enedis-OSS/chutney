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
import fr.enedis.chutney.kotlin.dsl.CompareAction
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.spEL

val `Scenario execution with simple json value extraction` = Scenario(title = "Scenario execution with simple json value extraction") {
  Given("this scenario is saved") {
    createScenario("scenarioId",
        """
        {
            "when":{
                "sentence":"Put JSON value in context",
                "implementation":{
                    "task":"{\n type: context-put \n inputs: {\n entries: {\n content: {\n field1: value1 \n} \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Put value in context with JSON extraction",
                    "implementation":{
                        "task":"{\n type: context-put \n inputs: {\n entries: {\n extracted: \"\${'$'}{#json(#content, '${'$'}.field1')}\" \n} \n} \n}"
                    }
                }
            ]
        }
        """.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL,"DEFAULT")
  }
  Then("the report status is SUCCESS") {
    checkScenarioReportSuccess()
  }
  And("the extracted value is 'value1'") {
    CompareAction(
        mode = "equals",
        actual = "json(#report, \"$.report.steps[1].stepOutputs.extracted\")".spEL,
        expected = "value1"
    )
  }
}

val `Scenario execution with multiple json value extraction` = Scenario(title = "Scenario execution with multiple json value extraction") {
  Given("this scenario is saved") {
    createScenario("scenarioId",
        """
        {
            "when":{
                "sentence":"Put JSON value in context",
                "implementation":{
                    "task":"{\n type: context-put \n inputs: {\n entries: {\n content: {\n field1: value1 \n field2: {\n field1: value1 \n} \n} \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Put value in context with JSON extraction",
                    "implementation":{
                        "task":"{\n type: context-put \n inputs: {\n entries: {\n extracted: \"\${'$'}{#json(#content, '${'$'}..field1')}\" \n} \n} \n}"
                    }
                }
            ]
        }
        """.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL,"DEFAULT")
  }
  Then("the report status is SUCCESS") {
    checkScenarioReportSuccess()
  }
  And("the extracted value is '[\"value1\",\"value1\"]'") {
    CompareAction(
        mode = "equals",
        actual = "json(#report, \"$.report.steps[1].stepOutputs.extracted[0]\")".spEL,
        expected = "value1"
    )
  }
}

val `Scenario execution with json object value extraction` = Scenario(title = "Scenario execution with json object value extraction") {
  Given("this scenario is saved") {
    createScenario("scenarioId",
        """
        {
            "when":{
                "sentence":"Put JSON value in context",
                "implementation":{
                    "task":"{\n type: context-put \n inputs: {\n entries: {\n content: {\n field1: value1 \n} \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Put value in context with JSON extraction",
                    "implementation":{
                        "task":"{\n type: context-put \n inputs: {\n entries: {\n extracted: \"\${'$'}{#json(#content, '${'$'}')}\" \n} \n} \n}"
                    }
                }
            ]
        }
        """.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL,"DEFAULT")
  }
  Then("the report status is SUCCESS") {
    checkScenarioReportSuccess()
  }
  And("the extracted value is '{field1=value1}'") {
    CompareAction(
        mode = "equals",
        actual = "json(#report, \"$.report.steps[1].stepOutputs.extracted.field1\")".spEL,
        expected = "value1"
    )
  }
}
