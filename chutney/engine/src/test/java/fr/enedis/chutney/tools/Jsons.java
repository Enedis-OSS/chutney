/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class Jsons {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private Jsons() {
    }

    public static <T> T loadJsonFromClasspath(String path, Class<T> targetClass) {
        return OBJECT_MAPPER.readValue(Jsons.class.getClassLoader().getResourceAsStream(path), targetClass);
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }
}
