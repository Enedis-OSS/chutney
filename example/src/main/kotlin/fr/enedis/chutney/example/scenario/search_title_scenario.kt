/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.example.scenario

import fr.enedis.chutney.kotlin.dsl.AssertAction
import fr.enedis.chutney.kotlin.dsl.HttpGetAction
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.spEL

val search_title_scenario = Scenario(title = "Search title displays") {
    When("I visit a search engine") {
        HttpGetAction(
            target = "search_engine",
            uri = "/",
            validations = mapOf("request accepted" to "status == 200".spEL()),
            outputs = mapOf("resultJson" to "body".spEL())
        )
    }
    Then("The search engine title is displayed") {
        AssertAction(
            listOf(
                "resultJson.contains('<title>Google</title>')".spEL()
            )
        )
    }
}
