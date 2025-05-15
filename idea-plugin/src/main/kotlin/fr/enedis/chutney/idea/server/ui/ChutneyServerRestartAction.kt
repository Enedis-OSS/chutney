/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.server.ui

import fr.enedis.chutney.idea.server.ChutneyServerRegistry
import fr.enedis.chutney.idea.server.ChutneyServerSettingsManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ChutneyServerRestartAction(private val mySession: ChutneyToolWindowSession) : AnAction() {
    override fun update(e: AnActionEvent) {
        val runningServer = ChutneyServerRegistry.instance.myServer
        val presentation = e.presentation
        if (runningServer != null && runningServer.isProcessRunning) {
            presentation.icon = AllIcons.Actions.Restart
            presentation.text = "Rerun local server"
        } else {
            presentation.icon = AllIcons.Actions.Execute
            presentation.text = "Start a local server"
        }
        e.presentation.isEnabled = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        mySession.saveSettings()
        mySession.restart(ChutneyServerSettingsManager.loadSettings())
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
      return ActionUpdateThread.BGT
    }

}
