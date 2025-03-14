/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.kotlin.dsl

const val JSON_PATH_ROOT = "\$"
val String.spELVar: String
    get() = "#$this"
val String.spEL: String
    get() = "\${#$this}"
val String.hjsonSpEL: String
    get() = "\\\${#$this}"

fun String.spEL(): String = "\${#$this}"
fun String.spELVar(): String = "#$this"
fun String.toSpelPair(): Pair<String, String> = this to this.spEL

fun String.elEval(): String = "\${$this}"
fun String.elString(): String = "'${this.replace("'", "''")}'"
fun Map<String, String>.elMap(): String {
    return this.map {
        e -> "${e.key.elString()}: ${e.value.elString()}"
    }.joinToString(prefix = "{", postfix = "}")
}
