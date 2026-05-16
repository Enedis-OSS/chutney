/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import static java.util.Objects.requireNonNull;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import fr.enedis.chutney.action.common.JsonUtils;
import fr.enedis.chutney.action.spi.SpelFunction;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JsonFunctions {

    private static final ObjectMapper om = new ObjectMapper();

    @Deprecated
    @SpelFunction
    public static Object json(Object document, String jsonPath) {
        return jsonPath(document, jsonPath);
    }

    @SpelFunction
    public static Object jsonPath(Object document, String jsonPath) {
        return JsonPath.parse(JsonUtils.jsonStringify(document)).read(jsonPath);
    }

    @SpelFunction
    public static String jsonSerialize(Object obj) throws JacksonException {
        return om.writeValueAsString(requireNonNull(obj));
    }

    @SpelFunction
    public static String jsonSet(Object document, String path, Object value) {
        return JsonPath.parse(JsonUtils.jsonStringify(document))
            .set(path, value)
            .jsonString();
    }

    @SpelFunction
    public static String jsonSetMany(Object document, Map<String, Object> map) {
        DocumentContext jsonDocument = JsonPath.parse(JsonUtils.jsonStringify(document));
        map.forEach(jsonDocument::set);
        return jsonDocument.jsonString();
    }

    @SpelFunction
    public static String jsonMerge(Object documentA, Object documentB) {
        LinkedHashMap jsonDocA = JsonPath.parse(JsonUtils.jsonStringify(documentA)).json();
        LinkedHashMap jsonDocB = JsonPath.parse(JsonUtils.jsonStringify(documentB)).json();

        jsonDocA.putAll(jsonDocB);

        return JsonPath.parse(JsonUtils.jsonStringify(jsonDocA)).jsonString();
    }

}
