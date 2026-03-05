/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.util

import org.apache.hc.client5.http.auth.BearerToken
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpCredentialsFactoryTest {

    @Test
    fun buildBasicAuth() {
        val chutneyServerInfo = ChutneyServerInfo(
            "http://localhost",
            "user",
            "password"
        )

        val credentials = credentials(chutneyServerInfo)

        assertThat(credentials).isInstanceOf(UsernamePasswordCredentials::class.java)
        assertThat((credentials as UsernamePasswordCredentials).userName)
            .isEqualTo("user")
        assertThat(String(credentials.userPassword))
            .isEqualTo("password")
    }

    @Test
    fun buildBearerAuth() {
        val chutneyServerInfo = ChutneyServerInfo.createWithToken(
            "http://localhost",
            "=Za0"
        )

        val credentials = credentials(chutneyServerInfo)

        assertThat(credentials).isInstanceOf(BearerToken::class.java)
        assertThat((credentials as BearerToken).token)
            .isEqualTo("=Za0")
    }

}
