/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.completion.field

import com.intellij.codeInsight.completion.CompletionResultSet
import fr.enedis.chutney.idea.completion.ChutneyJsonCompletionHelper
import java.util.*

object ChutneyJsonFieldCompletionFactory {
    fun from(
        completionHelper: ChutneyJsonCompletionHelper,
        completionResultSet: CompletionResultSet?
    ): Optional<FieldCompletion> {
        return if (completionHelper.completeHeadersKey()) {
            Optional.of(HeadersCompletion(completionHelper, completionResultSet))
        } else {
            Optional.empty()
        }
    }
}
