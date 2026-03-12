/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.authentication

sealed interface AuthMethod {

    data class Basic(val user: String,
                     val password: String): AuthMethod

    data class Bearer(val token: String): AuthMethod
}
