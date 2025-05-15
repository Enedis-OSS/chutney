/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin

import fr.enedis.chutney.kotlin.execution.CHUTNEY_ENV_ROOT_PATH_DEFAULT
import fr.enedis.chutney.kotlin.execution.report.CHUTNEY_REPORT_ROOT_PATH_DEFAULT

enum class ChutneyConfigurationParameters(val parameter: String, private val default: Any? = null) {
    /**
     * Properties to configure junit engine and launcher
     */
    CONFIG_REPORT_ROOT_PATH("chutney.report.rootPath", CHUTNEY_REPORT_ROOT_PATH_DEFAULT),
    CONFIG_ENVIRONMENT_ROOT_PATH("chutney.environment.rootPath", CHUTNEY_ENV_ROOT_PATH_DEFAULT),
    CONFIG_ENVIRONMENT("chutney.environment.default", null),

    /**
     * Properties to configure junit engine only
     */
    CONFIG_ENGINE_STEP_AS_TEST("chutney.engine.stepAsTest", true),
    CONFIG_REPORT_FILE("chutney.report.file.enabled", true),
    CONFIG_REPORT_SITE("chutney.report.site.enabled", false),
    CONFIG_CONSOLE_LOG_COLOR("chutney.log.color.enabled", true),
    CONFIG_SCENARIO_LOG("chutney.log.scenario.enabled", true),
    CONFIG_SCENARIO_LOG_ONLY_FAILED("chutney.log.scenario.onlyFailure", true),
    CONFIG_STEP_LOG("chutney.log.step.enabled", false);

    fun defaultBoolean(): Boolean {
        return when (this.default) {
            is Boolean -> this.default
            is String -> this.default.toBoolean()
            else -> false
        }
    }

    fun defaultNumber(): Number {
        return when (this.default) {
            is Number -> this.default
            else -> 0
        }
    }

    fun defaultString(): String? {
        return when (this.default) {
            is String -> this.default
            else -> this.default?.toString()
        }
    }
}
