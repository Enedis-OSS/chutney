/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.synchronize.server

import com.github.tomakehurst.wiremock.client.WireMock.*
import fr.enedis.chutney.kotlin.synchronize.ChutneyServerServiceImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class EnvironmentsTest : ChutneyServerServiceImplTest() {
    @Test
    fun get_environments() {
        wireMockServer.stubFor(
            get(urlPathMatching("/api/v2/environment"))
                .willReturn(
                    okJson("[]")
                )
        )

        val environments = ChutneyServerServiceImpl.getEnvironments(buildServerInfo())

        wireMockServer.verify(
            1,
            getRequestedFor(urlPathMatching("/api/v2/environment"))
        )
        Assertions.assertThat(environments).isEmpty()
    }
}
