/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.completion.value.model

import fr.enedis.chutney.idea.completion.value.StepValueData

class StepValue(val data: StepValueData) : Value(data.name) {
    override val isQuotable: Boolean
        get() = true

}
