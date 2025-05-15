/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.completion.value

import fr.enedis.chutney.idea.completion.CompletionHelper
import fr.enedis.chutney.idea.completion.TargetsValueCompletionHelper
import fr.enedis.chutney.idea.completion.value.model.StringValue
import fr.enedis.chutney.idea.completion.value.model.Value
import com.intellij.codeInsight.completion.CompletionResultSet
import java.util.function.Consumer

internal class TargetsValueCompletion(completionHelper: CompletionHelper, completionResultSet: CompletionResultSet) :
    ValueCompletion(completionHelper, completionResultSet) {
    override fun fill() {
        targets.forEach(Consumer { value: Value -> addValue(value) })
    }

    private val targets: List<Value>
        get() = TargetsValueCompletionHelper.targets
            .map { it.name }
            .map { value: String? -> StringValue(value!!) }
            .toList()
}
