/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.andrz.jackson.JsonReferenceProcessor
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File

object ChutneyUtil {

  private val mapper: ObjectMapper = ObjectMapper()
  private val jsonReferenceProcessor = JsonReferenceProcessor().apply {
    maxDepth = -1
    isPreserveRefs = true
    // isCacheInMemory = true
  }


  fun processJsonReference(jsonVirtualFile: VirtualFile): String {
    val node = jsonReferenceProcessor.process(File(jsonVirtualFile.path))
    return mapper.writeValueAsString(node)
  }

  fun isChutneyJson(jsonPsi: PsiFile): Boolean {
    return jsonPsi.name.indexOf("scenario.json") > 1 || jsonPsi.name.indexOf("chutney.json") > 1
  }

  fun isChutneyYaml(jsonPsi: PsiFile): Boolean {
    return jsonPsi.name.indexOf("chutney.yml") > 1 || jsonPsi.name.indexOf("chutney.yaml") > 1
  }

  private fun isChutneyDsl(ktPsi: PsiFile?): Boolean {
    return (ktPsi?.name?.indexOf(".kt") ?: 0) > 1
  }

  fun isChutneyDslMethod(psiElement: PsiElement): Boolean {
    return isChutneyDsl(psiElement.containingFile) &&
        (psiElement is KtNamedFunction) &&
        hasAnnotation(psiElement, "KChutney")
  }

  private fun hasAnnotation(psiElement: KtNamedFunction, annotationName: String): Boolean {
    return psiElement.annotationEntries.find { it.shortName?.asString().equals(annotationName) } != null

  }

  fun isChutneyDsl(ktFile: VirtualFile): Boolean {
    return ktFile.name.indexOf(".kt") > 1
  }


  fun isIcefragJson(jsonPsi: PsiFile): Boolean {
    return jsonPsi.name.indexOf("icefrag.json") > 1
  }

  fun isChutneyJson(virtualFile: VirtualFile): Boolean {
    return virtualFile.name.indexOf("scenario.json") > 1 || virtualFile.name.indexOf("chutney.json") > 1
  }

  fun getChutneyScenarioIdFromFileName(fileName: String): Int? {
    val dashIndex = fileName.indexOf("-")
    return try {
      if (dashIndex > 0) Integer.valueOf(fileName.substring(0, dashIndex)) else null
    } catch (e: Exception) {
      null
    }
  }

  fun getChutneyScenarioDescriptionFromFileName(fileName: String): String {
    val description = fileName.substring(0, fileName.indexOf(".chutney.json"))
    if (description.contains("-")) {
      return description.substring(description.indexOf("-") + 1)
    }
    return description
  }

  fun isRemoteChutneyJson(jsonPsi: PsiFile): Boolean {
    return isChutneyJson(jsonPsi) && getChutneyScenarioIdFromFileName(jsonPsi.name) != null
  }

  fun isRemoteChutneyJson(jsonVF: VirtualFile): Boolean {
    return isChutneyJson(jsonVF) && getChutneyScenarioIdFromFileName(jsonVF.name) != null
  }

  fun isChutneyFragmentsJson(virtualFile: VirtualFile): Boolean {
    return virtualFile.name.indexOf("icefrag.json") > 1
  }
}
