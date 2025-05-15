/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.annotations

import org.junit.platform.commons.annotation.Testable

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class ChutneyTest(val environment: String = "")
