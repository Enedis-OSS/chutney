/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.runner.settings

import fr.enedis.chutney.idea.runner.TestType
import fr.enedis.chutney.idea.runner.settings.ui.ChutneyVariablesData

data class ChutneyRunSettings(
    var directory: String = "",
    var scenarioFilePath: String = "",
    var scenariosFilesPath: String = "",
    var methodName: String = "",
    var variables: ChutneyVariablesData = ChutneyVariablesData.create(mapOf()),
    var serverType: ServerType = ServerType.INTERNAL,
    var serverAddress: String = "",
    var testType: TestType = TestType.SCENARIO_FILE
) {
    fun isExternalServerType(): Boolean {
        return serverType === ServerType.EXTERNAL
    }
}
