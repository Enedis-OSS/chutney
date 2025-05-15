/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.demo.sync

import fr.enedis.chutney.demo.spec.ArxivSpecs
import fr.enedis.chutney.demo.spec.ChutneyDBSpecs
import fr.enedis.chutney.demo.spec.SWAPISpecs
import fr.enedis.chutney.kotlin.util.ChutneyServerInfo

fun main() {
    DemoServer.synchronize()
}

object DemoServer {
    val CHUTNEY_DEMO = ChutneyServerInfo(
        url = "http://localhost",
        user = "admin",
        password = "Admin"
    )

    const val ENVIRONMENT_DEMO = "DEMO"

    fun synchronize() {
        SWAPISpecs.synchronize(CHUTNEY_DEMO, ENVIRONMENT_DEMO)
        ArxivSpecs.synchronize(CHUTNEY_DEMO, ENVIRONMENT_DEMO)
        ChutneyDBSpecs.synchronize(CHUTNEY_DEMO, ENVIRONMENT_DEMO)
    }
}
