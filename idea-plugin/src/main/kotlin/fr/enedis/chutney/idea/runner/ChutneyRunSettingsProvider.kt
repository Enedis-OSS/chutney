/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.runner

import fr.enedis.chutney.idea.runner.settings.ChutneyRunSettings
import com.intellij.psi.PsiElement

interface ChutneyRunSettingsProvider {
    fun provideSettings(psiElement: PsiElement): ChutneyRunSettings?
}
