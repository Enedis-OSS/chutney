/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.junit.engine.execution

import fr.enedis.chutney.kotlin.ChutneyConfigurationParameters
import fr.enedis.chutney.kotlin.ChutneyConfigurationParameters.*
import fr.enedis.chutney.kotlin.execution.report.AnsiReportWriter
import fr.enedis.chutney.kotlin.execution.report.JsonReportWriter
import fr.enedis.chutney.kotlin.execution.report.SiteGenerator
import fr.enedis.chutney.kotlin.junit.engine.execution.ChutneyJUnitReportingKeys.*
import fr.enedis.chutney.kotlin.util.SystemEnvConfigurationParameters
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

class ConsoleLogScenarioReportExecutionListener :
    EnabledTestExecutionListener(enabledProperty = listOf(CONFIG_SCENARIO_LOG, CONFIG_SCENARIO_LOG_ONLY_FAILED)) {

    private var ansiReportWriter: AnsiReportWriter? = null

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        super.testPlanExecutionStarted(testPlan)

        if (enabled()) {
            ansiReportWriter = AnsiReportWriter(
                configurationParameters().getBoolean(CONFIG_CONSOLE_LOG_COLOR.parameter)
                    .orElse(CONFIG_CONSOLE_LOG_COLOR.defaultBoolean())
            )
        }
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        entry?.let {
            printScenarioReport(it)
        }
    }

    private fun printScenarioReport(entry: ReportEntry) {
        entry.keyValuePairs[REPORT_JSON_STRING.value]?.let {
            if (enabled(CONFIG_SCENARIO_LOG)) {
                ansiReportWriter!!.printReport(JsonReportWriter.jsonAsReport(it))
            } else {
                entry.keyValuePairs[REPORT_STATUS_SUCCESS.value]?.let { success ->
                    if (enabled(CONFIG_SCENARIO_LOG_ONLY_FAILED) && !success.toBoolean()) {
                        ansiReportWriter!!.printReport(JsonReportWriter.jsonAsReport(it))
                    }
                }
            }
        }
    }
}

class ConsoleLogStepReportExecutionListener : EnabledTestExecutionListener(enabledProperty = listOf(CONFIG_STEP_LOG)) {

    private var ansiReportWriter: AnsiReportWriter? = null

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        super.testPlanExecutionStarted(testPlan)

        if (enabled()) {
            ansiReportWriter = AnsiReportWriter(
                configurationParameters().getBoolean(CONFIG_CONSOLE_LOG_COLOR.parameter)
                    .orElse(CONFIG_CONSOLE_LOG_COLOR.defaultBoolean())
            )
        }
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        entry?.let {
            printStepReport(it)
        }
    }

    private fun printStepReport(entry: ReportEntry) {
        entry.keyValuePairs[REPORT_STEP_JSON_STRING.value]?.let {
            if (enabled()) {
                val stepReport = JsonReportWriter.jsonAsReport(it)
                if (stepReport.steps.isEmpty()) {
                    ansiReportWriter!!.printStep(stepReport)
                }
            }
        }
    }
}

class FileWriterScenarioReportExecutionListener :
    EnabledTestExecutionListener(enabledProperty = listOf(CONFIG_REPORT_FILE)) {

    private var reportRootPathConfig: String? = null

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        super.testPlanExecutionStarted(testPlan)

        if (enabled()) {
            reportRootPathConfig =
                configurationParameters().get(CONFIG_REPORT_ROOT_PATH.parameter)
                    .orElse(CONFIG_REPORT_ROOT_PATH.defaultString())
        }
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        entry?.let {
            writeScenarioReport(it)
        }
    }

    private fun writeScenarioReport(entry: ReportEntry) {
        entry.keyValuePairs[REPORT_JSON_STRING.value]?.let {
            if (enabled()) {
                val report = JsonReportWriter.jsonAsReport(it)
                JsonReportWriter.writeReport(
                    report,
                    reportRootPathConfig!!,
                    true
                )
            }
        }
    }
}

class SiteGeneratorExecutionListener : EnabledTestExecutionListener(enabledProperty = listOf(CONFIG_REPORT_SITE)) {

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        if (enabled()) {
            SiteGenerator().generateSite()
        }
    }
}

abstract class EnabledTestExecutionListener(
    private val enabledProperty: List<ChutneyConfigurationParameters>
) : TestExecutionListener {
    private var configurationParameters: SystemEnvConfigurationParameters? = null
    private var enabled: Map<ChutneyConfigurationParameters, Boolean> = emptyMap()

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        configurationParameters = SystemEnvConfigurationParameters()
        enabled = enabledProperty.associateWith {
            configurationParameters!!.getBoolean(it.parameter).orElse(it.defaultBoolean())
        }
    }

    protected fun configurationParameters(): SystemEnvConfigurationParameters {
        return configurationParameters!!
    }

    protected fun enabled(): Boolean {
        return enabled.values.reduce(Boolean::or)
    }

    protected fun enabled(enableProperty: ChutneyConfigurationParameters): Boolean {
        return enabled[enableProperty] ?: false
    }
}
