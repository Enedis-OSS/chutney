/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.server.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import fr.enedis.chutney.idea.server.ChutneyServerRegistry

class ChutneyServerStopAction : AnAction("Stop the local server", null, AllIcons.Actions.Suspend) {
    override fun update(e: AnActionEvent) {
        val server = ChutneyServerRegistry.instance.myServer
        e.presentation.isEnabled = server != null && server.isProcessRunning
    }

    override fun actionPerformed(e: AnActionEvent) {
        val server = ChutneyServerRegistry.instance.myServer
        server?.shutdownAsync()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
      return ActionUpdateThread.BGT
    }
}
