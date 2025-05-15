/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.runner.settings.ui


import fr.enedis.chutney.idea.runner.settings.ChutneyRunSettings
import com.intellij.ui.PanelWithAnchor
import javax.swing.JComponent

interface RunSettingsSection : PanelWithAnchor {

    fun resetFrom(runSettings: ChutneyRunSettings)

    fun applyTo(runSettings: ChutneyRunSettings)

    fun getComponent(creationContext: CreationContext): JComponent
}
