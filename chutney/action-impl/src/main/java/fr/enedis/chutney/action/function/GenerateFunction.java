/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import fr.enedis.chutney.action.spi.SpelFunction;

public class GenerateFunction {

    @SpelFunction
    public static Generate generate() {
        return new Generate();
    }
}
