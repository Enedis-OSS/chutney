/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.tests.actions

import fr.enedis.chutney.acceptance.common.*
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.hjsonSpEL
import fr.enedis.chutney.kotlin.dsl.spEL

val `Kafka basic publish wrong url failure` = Scenario(title = "Kafka basic publish wrong url failure") {
  Given("A target pointing to a non unknown service") {
    createEnvironment(
      "KAFKA_ENV_KO",
      """
        [
            {
                "name": "test_kafka",
                "url": "tcp://$UNKNOWN_TARGET"
            }
        ]  
        """.trimIndent()
    )
  }
  And("This scenario with kafka tasks is saved") {
    createScenario(
      "scenarioId",
      """
        {
            "when":{
                "sentence":"Publish to broker",
                "implementation":{
                    "task":"{\n type: kafka-basic-publish \n target: test_kafka \n inputs: {\n topic: a-topic \n payload: bodybuilder \n} \n}"
                }
            },
            "thens":[]
        }  
        """.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL, "KAFKA_ENV_KO")
  }
  Then("the report status is FAILURE") {
    checkScenarioReportFailure()
  }
}

val `Kafka basic publish success` = Scenario(title = "Kafka basic publish wrong url failure") {
  Given("an associated target test_kafka having url in system property spring.embedded.kafka.brokers") {
    createEnvironment(
      "KAFKA_ENV_OK",
      """
        [
            {
                "name": "test_kafka",
                "url": "tcp://localhost:9092"
            }
        ]
        """.trimIndent()
    )
  }
  And("This scenario with kafka tasks is saved") {
    createScenario(
      "scenarioId",
      scenario = """
        {
            "givens": [
            {
                "sentence":"start server",
                "implementation":{
                    "task":"{\n type: kafka-broker-start \n target: test_kafka \n inputs: {\n topic: ['a-topic'] \n port: 9092 \n} \n}"
                }
            }
            ],
            "when":{
                "sentence":"Publish to broker",
                "implementation":{
                    "task":"{\n type: kafka-basic-publish \n target: test_kafka \n inputs: {\n topic: a-topic \n payload: bodybuilder \n headers: {\n X-API-VERSION: '1.0' \n} \n} \n}"
                }
            },
            "thens":[
                {
                    "sentence":"Consume from broker",
                    "implementation":{
                        "task":"{\n type: kafka-basic-consume \n target: test_kafka \n inputs: {\n topic: a-topic \n group: chutney \n ackMode: BATCH \n properties: {\n auto.offset.reset: earliest \n} \n} \n outputs: {\n payload : ${"payloads[0]".hjsonSpEL} \n} \n}"
                    }
                },
                {
                    "sentence":"Check payload",
                    "implementation":{
                        "task":"{\n type: string-assert \n inputs: {\n document: ${"payload".hjsonSpEL} \n expected: bodybuilder \n} \n}"
                    }
                }
            ]
        }
        """.trimIndent()
    )
  }
  When("The scenario is executed") {
    executeScenario("scenarioId".spEL, "KAFKA_ENV_OK")
  }
  Then("the report status is SUCCESS") {
    checkScenarioReportSuccess()
  }
}