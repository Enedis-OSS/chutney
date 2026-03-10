/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.util

import fr.enedis.chutney.kotlin.authentication.AuthMethod
import java.net.URL

data class ChutneyServerInfo(
    val url: String,
    val auth: AuthMethod?,
    val proxyUrl: String?,
    val proxyUser: String?,
    val proxyPassword: String?
) {
    @Deprecated(
        message = "This constructor is deprecated because authentication should now be provided via AuthMethod",
        replaceWith = ReplaceWith("ChutneyServerInfo(url, AuthMethod.Basic(user, password), proxyUrl, proxyUser, proxyPassword)")
    )
    constructor(
        url: String,
        user: String,
        password: String,
        proxyUrl: String?,
        proxyUser: String?,
        proxyPassword: String?
    ):
        this(
            url,
            AuthMethod.Basic(user, password),
            proxyUrl,
            proxyUser,
            proxyPassword
        )

    @Deprecated("This constructor is deprecated because authentication should now be provided via AuthMethod",
        replaceWith = ReplaceWith("ChutneyServerInfo(url, AuthMethod.Basic(user, password))"))
    constructor(url: String, user: String, password: String) :
        this(
            url,
            AuthMethod.Basic(user, password),
            proxyUrlFromProperties(),
            proxyUserFromProperties(),
            proxyPasswordFromProperties()
        )

    constructor(url: String, auth: AuthMethod?) :
        this(
            url,
            auth,
            proxyUrlFromProperties(),
            proxyUserFromProperties(),
            proxyPasswordFromProperties()
        )

    val uri: URL = URL(url)
    val proxyUri: URL? = proxyUrl?.let { URL(it) }

    fun user(): String? {
        return if(this.auth is AuthMethod.Basic) this.auth.user else "";
    }

    fun password(): String? {
        return if(this.auth is AuthMethod.Basic) this.auth.password else "";
    }

    fun isBasicAuth(): Boolean = auth is AuthMethod.Basic
    fun isTokenAuth(): Boolean = auth is AuthMethod.Bearer
}

private enum class ProxyProtocol { http, https }

private fun proxyUrlFromProperties(): String? {
    return ProxyProtocol.values()
        .firstNotNullOfOrNull {
            proxyUrlFromProperties(it, defaultPortResolver(it))
        }
}

private fun proxyUserFromProperties(): String? {
    return ProxyProtocol.values()
        .firstNotNullOfOrNull {
            proxyUserFromProperties(it)
        }
}

private fun proxyPasswordFromProperties(): String? {
    return ProxyProtocol.values()
        .firstNotNullOfOrNull {
            proxyPasswordFromProperties(it)
        }
}

private fun defaultPortResolver(scheme: ProxyProtocol): Int {
    return when (scheme) {
        ProxyProtocol.http -> 80
        ProxyProtocol.https -> 443
    }
}

private fun proxyUrlFromProperties(scheme: ProxyProtocol, defaultPort: Int): String? {
    val proxyHost = "${scheme}.proxyHost".sysProp()
    val proxyPort = "${scheme}.proxyPort".sysProp() ?: "$defaultPort"
    if (!proxyHost.isNullOrBlank()) {
        return "${scheme}://${proxyHost}:${proxyPort}"
    }
    return null
}

private fun proxyUserFromProperties(scheme: ProxyProtocol): String? {
    return "${scheme}.proxyUser".sysProp()
}

private fun proxyPasswordFromProperties(scheme: ProxyProtocol): String? {
    return "${scheme}.proxyPassword".sysProp()
}

private fun String.sysProp() = System.getProperty(this)
