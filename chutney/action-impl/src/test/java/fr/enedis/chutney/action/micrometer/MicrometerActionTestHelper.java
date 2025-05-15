/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.micrometer;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import java.util.Random;

final class MicrometerActionTestHelper {

    private static final Random rand = new Random();

    static void assertSuccessAndOutputObjectType(ActionExecutionResult result, String outputKey, Class clazz) {
        assertThat(result.status).isEqualTo(Success);
        assertThat(result.outputs).containsOnlyKeys(outputKey);
        assertThat(result.outputs)
            .extractingByKey(outputKey)
            .isInstanceOf(clazz);
    }

    static String buildMeterName(String prefix) {
        return prefix + "_" + rand.nextInt(10000);
    }
}
