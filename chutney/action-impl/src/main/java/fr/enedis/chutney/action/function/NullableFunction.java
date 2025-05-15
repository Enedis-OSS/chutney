/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import fr.enedis.chutney.action.spi.SpelFunction;
import java.util.Optional;

public class NullableFunction {

    @SpelFunction
    public static Object nullable(Object input) {
        return Optional.ofNullable(input).orElse("null");
    }

}
