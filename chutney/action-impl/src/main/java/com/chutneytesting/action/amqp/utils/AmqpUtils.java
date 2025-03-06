/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.amqp.utils;

import com.rabbitmq.client.LongString;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AmqpUtils {

    /**
     * Converts all values of the given Map and type LongString to String.
     * In case of map is null, null will be directly returned.
     *
     * @return consolidated map.
     */
    public static Map<String, Object> convertMapLongStringToString(Map<String, Object> map) {
        if (map == null) {
            return Collections.emptyMap();
        }

        return map.entrySet().stream()
            .map(e-> new AbstractMap.SimpleEntry<>(
                e.getKey(),
                Optional.ofNullable(convertLongStringToString(e.getValue())).orElse("null")))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Converts the given object in case of a LongString or List including a LongString to String.
     *
     * @return consolidated object
     */
    private static Object convertLongStringToString(Object value) {
        return switch (value) {
            case LongString ls -> ls.toString();
            case List lt -> {
                var newList = new ArrayList<>();
                for (Object item : lt) {
                    newList.add(convertLongStringToString(item));
                }
                yield newList;
            }
            case Map map -> convertMapLongStringToString(map);
            default -> value;
        };
    }
}
