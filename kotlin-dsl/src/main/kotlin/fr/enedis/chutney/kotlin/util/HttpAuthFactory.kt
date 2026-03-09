/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.util

import fr.enedis.chutney.kotlin.authentication.AuthMethod
import org.apache.hc.client5.http.auth.BearerToken
import org.apache.hc.client5.http.auth.Credentials
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials

fun credentials(chutneyServerInfo: ChutneyServerInfo): Credentials {
    return when (chutneyServerInfo.auth) {
      null -> UsernamePasswordCredentials(
          chutneyServerInfo.user,
          chutneyServerInfo.password.toCharArray()
      )
      is AuthMethod.Basic -> {
          UsernamePasswordCredentials(
              chutneyServerInfo.auth.user,
              chutneyServerInfo.auth.password.toCharArray()
          )
      }
        else -> {
            BearerToken((chutneyServerInfo.auth as AuthMethod.Bearer).token)
        }
    }
}

fun basicAuth(chutneyServerInfo: ChutneyServerInfo): Boolean {
    return (chutneyServerInfo.auth == null) || chutneyServerInfo.auth is AuthMethod.Basic
}
