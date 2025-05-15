/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.server

class ChutneyBrowserInfo(val id: String, val name: String) {

    override fun toString(): String {
        return "id=$id, name=$name"
    }

}
