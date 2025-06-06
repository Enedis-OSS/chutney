/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin

import java.net.URL

fun String.asResourceContent(): String = this.asResource().readText()

fun String.asResource(): URL = Thread.currentThread().contextClassLoader.getResource(this)
    ?: throw RuntimeException("Resource not found [$this]")
