/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

import fr.enedis.chutney.action.spi.SpelFunction;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;

public class TestFunctions {
    private static final Random RANDOM_GENERATOR = new Random();

    @SpelFunction
    public static String randomID() {
        return UUID.randomUUID().toString();
    }

    @SpelFunction
    public static String randomInt(int bound) {
        return String.valueOf(RANDOM_GENERATOR.nextInt(bound));
    }

    @SpelFunction
    public static ZonedDateTime now() {
        return ZonedDateTime.now();
    }

}
