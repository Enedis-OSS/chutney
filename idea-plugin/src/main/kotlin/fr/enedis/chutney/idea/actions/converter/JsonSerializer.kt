/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.actions.converter

import com.fasterxml.jackson.annotation.JsonInclude
import org.hjson.JsonValue
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.io.IOException

class JsonSerializer {

  private val mapper = jacksonMapperBuilder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .changeDefaultPropertyInclusion {   it.withValueInclusion(JsonInclude.Include.NON_NULL) }
    .build()



  fun toMap(rawScenarioV1: String?): MutableMap<String, Any?> {
    return try {
      val json = JsonValue.readHjson(rawScenarioV1).toString()
      mapper.readValue(json)
    } catch (e: IOException) {
      throw IllegalStateException(e)
    }
  }

  fun toString(map: Map<*, *>?): String {
    return try {
      mapper.writeValueAsString(map)
    } catch (e: JacksonException) {
      throw IllegalStateException(e)
    }
  }
}