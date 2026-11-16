/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.kotlin.launcher

import fr.enedis.chutney.kotlin.dsl.AssertAction
import fr.enedis.chutney.kotlin.dsl.ChutneyEnvironment
import fr.enedis.chutney.kotlin.dsl.Dataset
import fr.enedis.chutney.kotlin.dsl.Dataset.KeyValue
import fr.enedis.chutney.kotlin.dsl.ForStrategy
import fr.enedis.chutney.kotlin.dsl.Scenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LauncherTest {

    @Test
    fun `should run scenario with dataset`(@TempDir reports: Path) {
        val dataset = Dataset(
            name = "my dataset",
            uniqueValues = setOf(KeyValue("constant", "C")),
            multipleValues = listOf(
                listOf(KeyValue("key", "C")),
                listOf(KeyValue("key", "C"))
            )
        )
        val scenario = Scenario(title = "scenario with dataset") {
            When("<i> compare constant and iteration value", strategy = ForStrategy()) {
                AssertAction(listOf("\${#constant == #key}"))
            }
        }

        Launcher(reports.toString()).run(scenario, ChutneyEnvironment("env"), dataset = dataset)
    }
}
