/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.server

class ChutneyServerInfo(val serverUrl: String, capturedBrowsers: List<ChutneyBrowserInfo>) {
    val capturedBrowsers: List<ChutneyBrowserInfo> = capturedBrowsers

}
