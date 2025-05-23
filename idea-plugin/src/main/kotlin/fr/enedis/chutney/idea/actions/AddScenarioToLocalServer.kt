/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.actions

import fr.enedis.chutney.idea.ChutneyUtil
import fr.enedis.chutney.idea.ChutneyUtil.getChutneyScenarioDescriptionFromFileName
import fr.enedis.chutney.idea.ChutneyUtil.getChutneyScenarioIdFromFileName
import fr.enedis.chutney.idea.actions.converter.ScenarioV1ToV2Converter
import fr.enedis.chutney.idea.logger.EventDataLogger
import fr.enedis.chutney.idea.server.ChutneyServerRegistry
import fr.enedis.chutney.idea.util.HJsonUtils
import fr.enedis.chutney.idea.util.StringUtils.escapeSql
import fr.enedis.chutney.kotlin.util.ChutneyServerInfo
import fr.enedis.chutney.kotlin.util.HttpClient
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import org.apache.commons.text.StringEscapeUtils
import org.hjson.JsonValue
import org.hjson.Stringify

class AddScenarioToLocalServer : RemoteScenarioBaseAction() {

    private val LOG = Logger.getInstance(AddScenarioToLocalServer::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val id = getChutneyScenarioIdFromFileName(file.name) ?: return
        val titleAndDescription = getChutneyScenarioDescriptionFromFileName(file.name)
        try {
            val localServerURL = ChutneyServerRegistry.instance.myServer?.serverUrl ?: return
            val content = escapeSql(HJsonUtils.convertHjson(psiFile.text))

            val query = "/api/scenario/v2/raw"
            val body =
                "{\"id\": $id ,\"content\":\"${StringEscapeUtils.escapeJson(content)}\", \"title\": \"$titleAndDescription\", \"description\":\"$titleAndDescription\"}"

            val result = HttpClient.post<Any>(ChutneyServerInfo(localServerURL, "", ""), query, body)
            EventDataLogger.logInfo("Scenario Added to Local Server.<br>", project)
        } catch (e: Exception) {
            LOG.debug(e.toString())
            EventDataLogger.logError(e.toString(), project)
        }
    }
}
