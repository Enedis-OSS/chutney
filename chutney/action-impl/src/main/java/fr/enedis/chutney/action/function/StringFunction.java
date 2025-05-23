/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import fr.enedis.chutney.action.spi.SpelFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringFunction {

    @Deprecated
    @SpelFunction
    public static String str_replace(String input, String regularExpression, String replacement) {
        return stringReplace(input, regularExpression, replacement);
    }

    @SpelFunction
    public static String stringReplace(String input, String regularExpression, String replacement) {
        Matcher m = Pattern.compile(regularExpression).matcher(input);
        StringBuilder text = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(text, replacement);
        }
        m.appendTail(text);
        return text.toString();
    }
}
