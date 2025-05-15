/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.completion.contributor

import fr.enedis.chutney.idea.ChutneyUtil
import fr.enedis.chutney.idea.completion.ChutneyJsonCompletionHelper
import fr.enedis.chutney.idea.completion.ChutneyJsonPathResolver
import fr.enedis.chutney.idea.completion.JsonTraversal
import fr.enedis.chutney.idea.completion.field.ChutneyJsonFieldCompletionFactory
import fr.enedis.chutney.idea.completion.value.ChutneyJsonValueCompletionFactory
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet


class ChutneyJsonCompletionContributor : CompletionContributor() {

    private val jsonTraversal: JsonTraversal = JsonTraversal()

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (ChutneyUtil.isChutneyJson(parameters.originalFile)) {
            val psiElement = parameters.position
            val pathResolver = ChutneyJsonPathResolver()
            val completionHelper = ChutneyJsonCompletionHelper(psiElement, jsonTraversal, pathResolver)
            when {
                jsonTraversal.isKey(psiElement) -> ChutneyJsonFieldCompletionFactory.from(completionHelper, result)
                    .ifPresent { it.fill() }
                else -> ChutneyJsonValueCompletionFactory.from(completionHelper, result)
                    .ifPresent { it.fill() }
            }
        }

    }
}
