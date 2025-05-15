/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.runner.settings.ui

import fr.enedis.chutney.idea.runner.TestType

class TestTypeListItem(
    val testType: TestType,
    val displayName: String,
    private val myRunSettingsSection: RunSettingsSection
) : IdProvider, RunSettingsSectionProvider {

    override val id: String
        get() = testType.name

    override fun provideRunSettingsSection(): RunSettingsSection {
        return myRunSettingsSection
    }

}
