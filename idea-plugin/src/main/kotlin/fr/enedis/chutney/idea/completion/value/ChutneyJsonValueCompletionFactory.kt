/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.completion.value

import com.intellij.codeInsight.completion.CompletionResultSet
import fr.enedis.chutney.idea.completion.ChutneyJsonCompletionHelper
import java.util.*

object ChutneyJsonValueCompletionFactory {
    fun from(
        completionHelper: ChutneyJsonCompletionHelper,
        completionResultSet: CompletionResultSet
    ): Optional<ValueCompletion> {
        if (completionHelper.completeTargetsValue()) {
            return Optional.of(TargetsValueCompletion(completionHelper, completionResultSet))
        } else if (completionHelper.completeStepsValue()) {
            return Optional.of(StepsValueCompletion(completionHelper, completionResultSet))
        } else if (completionHelper.completeVariableValue()) {
            return Optional.of(VariableValueCompletion(completionHelper, completionResultSet))
        }
        return Optional.empty()
    }
}
