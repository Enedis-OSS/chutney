/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.common

import fr.enedis.chutney.engine.api.execution.StatusDto
import fr.enedis.chutney.kotlin.dsl.ChutneyStepBuilder
import fr.enedis.chutney.kotlin.dsl.CompareAction
import fr.enedis.chutney.kotlin.dsl.spEL

fun ChutneyStepBuilder.checkScenarioReportSuccess() {
  checkScenarioReportStatus(StatusDto.SUCCESS)
}

fun ChutneyStepBuilder.checkScenarioReportFailure() {
  checkScenarioReportStatus(StatusDto.FAILURE)
}

private fun ChutneyStepBuilder.checkScenarioReportStatus(status: StatusDto) {
  CompareAction(
    mode = "equals",
    actual = "json(#report, '$.report.status')".spEL,
    expected = status.name
  )
}

fun jsonHeader() = mapOf("Content-Type" to "application/json;charset=UTF-8")