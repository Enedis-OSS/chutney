/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.actions

import fr.enedis.chutney.idea.ChutneyUtil
import fr.enedis.chutney.idea.settings.ChutneySettings
import fr.enedis.chutney.kotlin.util.HttpClient
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*

class DynamicActionGroup : ActionGroup() {

    private fun getCampaigns() =
        HttpClient.get<List<Campaign>>(ChutneySettings.getInstance().state.serverInfo()!!, "/api/ui/campaign/v1")

    override fun getChildren(event: AnActionEvent?): Array<out AnAction> {
        val project = event?.project ?: return emptyArray()
        if (!ChutneySettings.checkRemoteServerUrlConfig(project)) return emptyArray()

        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return emptyArray()
        val id = ChutneyUtil.getChutneyScenarioIdFromFileName(file.name)
        val campaigns = getCampaigns()
        return campaigns
          .map { campaign ->
            val selected = campaign.scenarios.map { it.scenarioId }.contains(id)
            AddRemoveScenarioInCampaignAction(
                campaign,
                id!!,
                selected,
                campaign.id.toString() + "-" + campaign.title,
                campaign.title,
                if (selected) AllIcons.Actions.Checked_selected else null
            )
        }.toTypedArray()
    }

    override fun update(event: AnActionEvent) {
        // Enable/disable depending
        val project = event.project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val psiFile = event.getData(LangDataKeys.PSI_FILE)
        event.presentation.isEnabledAndVisible =
            project != null && psiFile != null && virtualFile != null && !virtualFile.isDirectory &&
                    ChutneyUtil.isRemoteChutneyJson(psiFile)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT;
    }
}
