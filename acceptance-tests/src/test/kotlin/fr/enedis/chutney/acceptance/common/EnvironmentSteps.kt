/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.common

import fr.enedis.chutney.kotlin.dsl.ChutneyStepBuilder
import fr.enedis.chutney.kotlin.dsl.HttpPostAction
import fr.enedis.chutney.kotlin.dsl.statusValidation

const val UNKNOWN_TARGET = "unknownhost:12345"

fun ChutneyStepBuilder.createEnvironment(environmentName: String, targets: String) {
  HttpPostAction(
    target = "CHUTNEY_LOCAL",
    uri = "/api/v2/environments",
    headers = jsonHeader(),
    body = """
            {
                "name": "$environmentName",
                "description": "",
                "targets": $targets
            }
           """,
    validations = mapOf(
      statusValidation(200)
    )
  )
}