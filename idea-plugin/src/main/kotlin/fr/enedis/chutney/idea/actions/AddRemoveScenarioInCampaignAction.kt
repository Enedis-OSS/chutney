/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.actions

import fr.enedis.chutney.idea.logger.EventDataLogger
import fr.enedis.chutney.idea.settings.ChutneySettings
import fr.enedis.chutney.kotlin.util.HttpClient
import com.google.gson.Gson
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class AddRemoveScenarioInCampaignAction(
  private val campaign: Campaign,
  private val scenarioId: Int,
  private val selected: Boolean,
  text: String?,
  description: String?,
  icon: Icon?
) : AnAction(text, description, icon) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!ChutneySettings.checkRemoteServerUrlConfig(project)) return
        try {
            var campaignScenarios: MutableList<Campaign.CampaignScenario> = campaign.scenarios.toMutableList()
            if (selected) {
                //remove from campaign
                campaignScenarios.remove(Campaign.CampaignScenario(scenarioId))

            } else {
                //add to campaign
                campaignScenarios = campaignScenarios.plus(Campaign.CampaignScenario(scenarioId)).toMutableList()
            }
            val serverInfo = ChutneySettings.getInstance().state.serverInfo()!!
            HttpClient.put<Any>(serverInfo,"/api/ui/campaign/v1", Gson().toJson(campaign.copy(scenarios = campaignScenarios)))

            EventDataLogger.logInfo(
                "scenario" + (if (selected) " removed from" else " added to") + " campaign with success.<br>" +
                        "<a href=\"${serverInfo.url}/#/campaign/${campaign.id}/executions\">Open Campaign in remote Chutney Server</a>",
                project,
                NotificationListener.URL_OPENING_LISTENER
            )
        } catch (e: Exception) {
            EventDataLogger.logError(e.toString(), project)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
      return ActionUpdateThread.BGT
    }
}
