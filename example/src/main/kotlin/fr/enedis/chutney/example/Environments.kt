/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package fr.enedis.chutney.example

import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.ChutneyTarget

val google_fr = ChutneyTarget(name = "search_engine", url = "https://www.google.fr")

val environment_fr = ChutneyEnvironment(
    name = "The French World Wide Web",
    description = "The World Wide Web, for strange but beautiful French people",
    targets = listOf(
        google_fr
    )
)

val google_en = ChutneyTarget(name = "search_engine", url = "https://www.google.com")

val environment_en = ChutneyEnvironment(
    name = "The English World Wide Web",
    description = "The World Wide Web, mostly",
    targets = listOf(
        google_en
    )
)
