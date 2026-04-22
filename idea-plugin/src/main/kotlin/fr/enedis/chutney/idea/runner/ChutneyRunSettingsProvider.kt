/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.runner

import com.intellij.psi.PsiElement
import fr.enedis.chutney.idea.runner.settings.ChutneyRunSettings

interface ChutneyRunSettingsProvider {
    fun provideSettings(psiElement: PsiElement): ChutneyRunSettings?
}
