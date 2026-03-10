/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.util

import fr.enedis.chutney.kotlin.authentication.AuthMethod
import org.apache.hc.client5.http.auth.BearerToken
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HttpAuthFactoryTest {

    @Nested
    @DisplayName("Credentials")
    inner class Credentials {

        @Test
        fun build_basic_auth() {
            val chutneyServerInfo = ChutneyServerInfo(
                "http://localhost", "user", "password"
            )

            val credentials = credentials(chutneyServerInfo)

            assertThat(credentials).isInstanceOf(UsernamePasswordCredentials::class.java)
            assertThat((credentials as UsernamePasswordCredentials).userName)
                .isEqualTo("user")
            assertThat(String(credentials.userPassword))
                .isEqualTo("password")
        }

        @Test
        fun build_bearer_auth() {
            val chutneyServerInfo = ChutneyServerInfo(
                "http://localhost",
                AuthMethod.Bearer("=Za0")
            )

            val credentials = credentials(chutneyServerInfo)

            assertThat(credentials).isInstanceOf(BearerToken::class.java)
            assertThat((credentials as BearerToken).token)
                .isEqualTo("=Za0")
        }
    }

    @Nested
    @DisplayName("IsBasicAuth")
    inner class IsBasicAuth {
        @Test
        fun user_password_is_basic_auth() {
            val chutneyServerInfo = ChutneyServerInfo(
                "http://localhost",
                "user",
                "password"
            )

            assertThat(basicAuth(chutneyServerInfo)).isTrue
        }

        @Test
        fun bearer_auth_is_not_basic_auth() {
            val chutneyServerInfo = ChutneyServerInfo(
                "http://localhost",
                AuthMethod.Bearer("=Za0")
            )

            assertThat(basicAuth(chutneyServerInfo)).isFalse
        }
    }


}
