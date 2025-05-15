/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.server

class ChutneyServerFetchResult(val serverInfo: ChutneyServerInfo?, val errorMessage: String?) {

    val isError: Boolean
        get() = errorMessage != null

    companion object {
        fun fromErrorMessage(errorMessage: String?): ChutneyServerFetchResult {
            return ChutneyServerFetchResult(null, errorMessage)
        }

        fun fromServerInfo(serverInfo: ChutneyServerInfo?): ChutneyServerFetchResult {
            return ChutneyServerFetchResult(serverInfo, null)
        }
    }

}
