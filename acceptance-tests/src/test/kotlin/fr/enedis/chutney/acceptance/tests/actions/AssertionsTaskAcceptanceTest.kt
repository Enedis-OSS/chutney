/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.tests.actions

import fr.enedis.chutney.acceptance.common.checkScenarioReportSuccess
import fr.enedis.chutney.acceptance.common.createScenario
import fr.enedis.chutney.acceptance.common.executeScenario
import fr.enedis.chutney.kotlin.dsl.ChutneyScenarioBuilder
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.spEL
import fr.enedis.chutney.kotlin.dsl.hjsonSpEL

val `Execution by UI controller` = Scenario(title = "Execution by UI controller") {
  micrometerScenario(
    """
        {
            "when":{
                "sentence":"Put values in context",
                "implementation":{
                    "task":"{\n type: context-put \n inputs: {\n entries: {\n test_int: {\"status\":{\"code\":200.0,\"reason\":\"OK\"}} \n test_xml: <test><xml attr=\"attributeValue\"><status><code>200.0</code><reason>OK</reason></status></xml></test> \n test_xml_ns: <tt:test xmlns:tt=\"http://test.org\" xmlns=\"http://test.org\"><xml attr=\"attributeValue\"><status xmlns=\"http://simple.org\"><code>200.0</code><reason>OK</reason></status></xml></test> \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Json assert",
                    "implementation":{
                        "task":"{\n type: json-assert \n inputs: {\n document: ${"test_int".hjsonSpEL} \n expected: {\n $.status.code: 200.0 \n $.status.reason: OK \n} \n} \n}"
                    }
                },
                {
                    "sentence":"Xml assert",
                    "implementation":{
                        "task":"{\n type: xml-assert \n inputs: {\n document: ${"test_xml".hjsonSpEL} \n expected: {\n /test/xml/@attr: attributeValue \n /test/xml/status/code: \"200.0\" \n /test/xml/status/reason: OK \n} \n} \n}"
                    }
                },
                {
                    "sentence":"Xml assert with namespace",
                    "implementation":{
                        "task":"{\n type: xml-assert \n inputs: {\n document: ${"test_xml_ns".hjsonSpEL} \n expected: {\n /test/xml/@attr: attributeValue \n /test/xml/status/code: \"200.0\" \n /test/xml/status/reason: OK \n} \n} \n}"
                    }
                }
            ]
        }
        """
  )
}

val `All in one assertions` = Scenario(title = "All in one assertions") {
  micrometerScenario(
    """
        {
            "when":{
                "sentence":"Put values in context",
                "implementation":{
                    "task":"{\n type: context-put \n inputs: {\n entries: {\n test_json: {\"status\":{\"code\":\"200\",\"reason\":\"OK\"}} \n test_int: {\"status\":{\"code\":200.0,\"reason\":\"OK\"}} \n test_json2: {\"test\":{\"status\":{\"code\":\"200\",\"reason\":\"OK\"}}} \n test_string: Sky is the limit \n test_xml: <test><xml><is>boring</is></xml></test> \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Simple asserts",
                    "implementation":{
                        "task":"{\n type: assert \n inputs: {\n asserts: [{\n assert-true: \${'$'}{1 == 1} \n} \n {\n assert-true: \${'$'}{!false} \n}] \n} \n}"
                    }
                },
                {
                    "sentence":"Json assert",
                    "implementation":{
                        "task":"{\n type: json-assert \n inputs: {\n document: ${"test_json".hjsonSpEL} \n expected: {\n $.status.code: \"200\" \n $.status.reason: OK \n $.status.not_exist: null \n} \n} \n}"
                    }
                },
                {
                    "sentence":"Json compare",
                    "implementation":{
                        "task":"{\n type: json-compare \n inputs: {\n document1: ${"test_json".hjsonSpEL} \n document2: ${"test_json2".hjsonSpEL} \n comparingPaths: {\n $.status: $.test.status \n $.status.code: $.test.status.code \n} \n} \n}"
                    }
                },
                {
                    "sentence":"Xml assert",
                    "implementation":{
                        "task":"{\n type: xml-assert \n inputs: {\n document: ${"test_xml".hjsonSpEL} \n expected: {\n /test/xml/is//text(): boring \n} \n} \n}"
                    }
                },
                {
                    "sentence":"Json assert",
                    "implementation":{
                        "task":"{\n type: json-assert \n inputs: {\n document: ${"test_int".hjsonSpEL} \n expected: {\n $.status.code: 200.0 \n} \n} \n}"
                    }
                }
            ]
        }
        """
  )
}

val `Test xsd actions` = Scenario(title = "Test xsd actions") {
  micrometerScenario(
    """
        {
            "when":{
                "sentence":"Validate employee xml with employee xsd",
                "implementation":{
                    "task":"{\n type: xsd-validation \n inputs: {\n xsd: file:/config/xsd_samples/employee.xsd \n xml: <?xml version=\"1.0\"?><Employee xmlns=\"https://www.chutneytesting.com/Employee\"><name>Pankaj</name><age>29</age><role>Java Developer</role><gender>Male</gender></Employee> \n} \n}"
                }
            },
            "thens":[]
        }
        """
  )
}

private fun ChutneyScenarioBuilder.micrometerScenario(scenario: String) {
  Given("this scenario is saved") {
    createScenario(
      "scenarioId",
      scenario.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL, "DEFAULT")
  }
  Then("the report status is SUCCESS") {
    checkScenarioReportSuccess()
  }
}