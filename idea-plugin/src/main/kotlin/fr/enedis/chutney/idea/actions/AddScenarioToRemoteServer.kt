/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.actions

import fr.enedis.chutney.idea.ChutneyUtil
import fr.enedis.chutney.idea.ChutneyUtil.getChutneyScenarioDescriptionFromFileName
import fr.enedis.chutney.idea.actions.converter.ScenarioV1ToV2Converter
import fr.enedis.chutney.idea.logger.EventDataLogger
import fr.enedis.chutney.idea.settings.ChutneySettings
import fr.enedis.chutney.idea.util.HJsonUtils
import fr.enedis.chutney.kotlin.util.HttpClient
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.refactoring.RefactoringFactory
import org.apache.commons.text.StringEscapeUtils
import org.hjson.JsonValue
import org.hjson.Stringify

class AddScenarioToRemoteServer : AnAction() {

    private val LOG = Logger.getInstance(AddScenarioToRemoteServer::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.project ?: return
        if (!ChutneySettings.checkRemoteServerUrlConfig(project)) return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val titleAndDescription = getChutneyScenarioDescriptionFromFileName(file.name)
        try {
            val serverInfo = ChutneySettings.getInstance().state.serverInfo()!!
            val processJsonReference = ChutneyUtil.processJsonReference(psiFile.virtualFile)
            var hJsonString: String? = JsonValue.readHjson(processJsonReference).toString(Stringify.PLAIN)
            val content = HJsonUtils.convertHjson(hJsonString)
            val query = "/api/scenario/v2/raw"
            val body =
                "{\"content\":\"${StringEscapeUtils.escapeJson(content)}\", \"title\": \"$titleAndDescription\", \"description\":\"$titleAndDescription\"}"
            val id = HttpClient.post<Long>(serverInfo, query, body)
            val createRename =
                RefactoringFactory.getInstance(project).createRename(psiFile, "$id-$titleAndDescription.chutney.json")
            WriteCommandAction.runWriteCommandAction(project) { createRename.run() }
            EventDataLogger.logInfo(
                "Scenario Added to Remote Server.<br>" +
                        "<a href=\"${serverInfo.url}/#/scenario/$id/executions?open=last&active=last\">Open in remote Chutney Server</a>",
                project,
                NotificationListener.URL_OPENING_LISTENER
            )

        } catch (e: Exception) {
            LOG.debug(e.toString())
            EventDataLogger.logError(e.toString(), project)
        }
    }

    override fun update(event: AnActionEvent) {
        // Set the availability based on whether a project is open
        val project = event.project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val psiFile = event.getData(LangDataKeys.PSI_FILE)
        event.presentation.isEnabledAndVisible =
            project != null && psiFile != null && virtualFile != null && !virtualFile.isDirectory && ChutneyUtil.isChutneyJson(
                psiFile
            )
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }
}
