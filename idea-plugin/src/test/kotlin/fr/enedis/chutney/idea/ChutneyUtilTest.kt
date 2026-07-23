/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea

import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class ChutneyUtilTest {

  @Test
  fun `should process json file and return formatted json`() {
    // Given
    val jsonFile = File.createTempFile("scenario", ".json")
    jsonFile.writeText(
      """
            {
              "name": "test"
            }
            """.trimIndent()
    )

    val virtualFile = mock<VirtualFile>()
    whenever(virtualFile.path).thenReturn(jsonFile.absolutePath)

    // When
    val result = ChutneyUtil.processJsonReference(virtualFile)

    // Thenf
    assertTrue(result.contains("\"name\""))
    assertTrue(result.contains("\"test\""))

    jsonFile.delete()
  }
}

