/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea

import com.intellij.json.JsonLanguage

class ChutneyLanguage : JsonLanguage("Chutney", "application/json") {
    companion object {
        val INSTANCE = ChutneyLanguage()
    }
}
