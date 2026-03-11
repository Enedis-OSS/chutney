/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.settings

import fr.enedis.chutney.idea.logger.EventDataLogger
import fr.enedis.chutney.kotlin.util.ChutneyServerInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import fr.enedis.chutney.kotlin.authentication.AuthMethod

@State(name = "ChutneySettings", storages = [Storage("chutney.xml")])
class ChutneySettings : PersistentStateComponent<ChutneySettings.ChutneySettingsState> {

    private var settingsState: ChutneySettingsState = ChutneySettingsState()

    class ChutneySettingsState(
      var url: String? = "",
      var basicAuth: Boolean? = true,
      var user: String? = "",
      var password: String? = "",
      var token: String? = "",
      var proxyUrl: String? = "",
      var proxyUser: String? = "",
      var proxyPassword: String? = ""
    ) {
        fun serverInfo(): ChutneyServerInfo? {
            if (!url.isNullOrBlank()) {
                return try {
                    ChutneyServerInfo(
                        url = url!!,
                      if(basicAuth == true) AuthMethod.Basic(user.toString(), password.toString())
                        else AuthMethod.Bearer(token.toString()),
                        proxyUrl = proxyUrl.takeIf { ! it.isNullOrBlank() },
                        proxyUser = proxyUser.takeIf { ! it.isNullOrBlank() },
                        proxyPassword = proxyPassword.takeIf { ! it.isNullOrBlank() }
                    )
                } catch (_: Exception) {
                    null;
                }
            }
            return null
        }
    }

    override fun getState(): ChutneySettingsState {
        return settingsState
    }

    override fun loadState(state: ChutneySettingsState) {
        settingsState = state
    }

    companion object {
        fun getInstance(): ChutneySettings {
            return ApplicationManager.getApplication().getService(ChutneySettings::class.java)
        }

        fun checkRemoteServerUrlConfig(project: Project): Boolean {
            val serverInfo = getInstance().state.serverInfo()
            if (serverInfo == null || serverInfo.url.isBlank()) {
                EventDataLogger.logError(
                    " <a href=\"configure\">Configure</a> Missing remote configuration server, please check url, authentication and proxy if needed",
                    project
                ) { _, _ ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Chutney")
                }
                return false
            }
            return true
        }
    }
}
