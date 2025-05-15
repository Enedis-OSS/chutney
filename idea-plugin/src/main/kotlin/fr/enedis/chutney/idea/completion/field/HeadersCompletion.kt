/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.completion.field

import fr.enedis.chutney.idea.completion.CompletionHelper
import fr.enedis.chutney.idea.completion.field.model.ChutneyJsonFields.headers
import fr.enedis.chutney.idea.completion.field.model.Field
import com.intellij.codeInsight.completion.CompletionResultSet
import java.util.function.Consumer

internal class HeadersCompletion(completionHelper: CompletionHelper?, completionResultSet: CompletionResultSet?) :
    FieldCompletion(completionHelper!!, completionResultSet!!) {
    override fun fill() {
        headers().forEach(Consumer { field: Field? -> addUnique(field!!) })
    }
}
