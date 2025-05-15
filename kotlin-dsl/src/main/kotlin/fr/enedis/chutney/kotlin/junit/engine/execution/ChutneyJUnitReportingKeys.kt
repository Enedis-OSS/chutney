/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine.execution

enum class ChutneyJUnitReportingKeys(val value: String) {
    REPORT_JSON_STRING("chutney.report"),
    REPORT_STEP_JSON_STRING("chutney.report.step"),
    REPORT_STATUS_SUCCESS("chutney.report.status.success")
}
