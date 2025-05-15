/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
*/


import fr.enedis.chutney.kotlin.dsl.ContextPutAction
import fr.enedis.chutney.kotlin.dsl.HttpGetAction
import fr.enedis.chutney.kotlin.dsl.JsonAssertAction
import fr.enedis.chutney.kotlin.dsl.RetryTimeOutStrategy
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.elEval

Scenario(title = "swapi GET people record") {
    Given("I set get people service api endpoint") {
        ContextPutAction(entries = mapOf("uri" to "api/people/1"))
    }
    When("I send GET HTTP request", RetryTimeOutStrategy("5 s", "1 s")) {
        HttpGetAction(target = "swapi.dev", uri = "\${#uri}", validations = mapOf("always true" to "true".elEval()))
    }
    Then("I receive valid HTTP response") {
        JsonAssertAction(document = "\${#body}", expected = mapOf("$.name" to "Luke Skywalker"))
    }
}
