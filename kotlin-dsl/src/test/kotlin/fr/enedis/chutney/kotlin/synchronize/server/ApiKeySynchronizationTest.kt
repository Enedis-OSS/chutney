/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.synchronize.server

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import fr.enedis.chutney.kotlin.authentication.AuthMethod
import fr.enedis.chutney.kotlin.dsl.Campaign
import fr.enedis.chutney.kotlin.dsl.Dataset
import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.SuccessAction
import fr.enedis.chutney.kotlin.synchronize.ChutneyServerServiceImpl
import fr.enedis.chutney.kotlin.util.ChutneyServerInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ApiKeySynchronizationTest : ChutneyServerServiceImplTest() {

    private val token = "personalToken"
    private val serverInfo get() = ChutneyServerInfo(wireMockServer.baseUrl(), AuthMethod.ApiKey(token))

    private fun MappingBuilder.withToken() = withHeader("X-API-KEY", equalTo(token))

    @AfterEach
    fun no_other_credentials_sent() {
        wireMockServer.allServeEvents.forEach {
            assertThat(it.request.containsHeader("Authorization")).isFalse()
        }
    }

    @Test
    fun create_scenario() {
        val scenario = Scenario(id = 123, title = "A scenario") { When("Something happens") { SuccessAction() } }
        wireMockServer.stubFor(get("/api/scenario/v2/raw/123").withToken().willReturn(notFound()))
        wireMockServer.stubFor(post("/api/scenario/v2/raw").withToken().willReturn(okJson("123")))

        val id = ChutneyServerServiceImpl.createOrUpdateJsonScenario(serverInfo, scenario)

        assertThat(id).isEqualTo(123)
    }

    @Test
    fun update_scenario() {
        val scenario = Scenario(id = 123, title = "A scenario") { When("Something happens") { SuccessAction() } }
        wireMockServer.stubFor(
            get("/api/scenario/v2/raw/123").withToken()
                .willReturn(okJson("""{"id": "123", "version": 4, "tags": []}"""))
        )
        wireMockServer.stubFor(
            post("/api/scenario/v2/raw").withToken()
                .withRequestBody(matchingJsonPath("$.version", equalTo("4")))
                .willReturn(okJson("123"))
        )

        val id = ChutneyServerServiceImpl.createOrUpdateJsonScenario(serverInfo, scenario)

        assertThat(id).isEqualTo(123)
    }

    @Test
    fun update_existing_dataset() {
        val dataset = Dataset(name = "MY_DATASET", description = "desc")
        wireMockServer.stubFor(
            get("/api/v1/datasets/${dataset.id}").withToken().willReturn(okJson("""{"id": "${dataset.id}"}"""))
        )
        wireMockServer.stubFor(
            put("/api/v1/datasets").withToken().willReturn(okJson("""{"id": "${dataset.id}"}"""))
        )

        val id = ChutneyServerServiceImpl.createOrUpdateDataset(serverInfo, dataset)

        assertThat(id).isEqualTo(dataset.id)
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/datasets")))
    }

    @Test
    fun get_environments() {
        wireMockServer.stubFor(get("/api/v2/environments").withToken().willReturn(okJson("[]")))

        assertThat(ChutneyServerServiceImpl.getEnvironments(serverInfo)).isEmpty()
    }

    @Test
    fun create_campaign() {
        wireMockServer.stubFor(post("/api/ui/campaign/v1").withToken().willReturn(okJson("""{"id": 7}""")))

        val id = ChutneyServerServiceImpl.createOrUpdateCampaign(
            serverInfo,
            Campaign(title = "campaign", environment = "DEFAULT")
        )

        assertThat(id).isEqualTo(7)
    }
}
