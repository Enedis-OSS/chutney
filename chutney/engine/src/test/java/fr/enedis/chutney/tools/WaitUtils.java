/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

import java.util.concurrent.TimeUnit;

public class WaitUtils {

    public static void awaitDuring(int mills, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(mills);
        } catch (InterruptedException e) {
            throw new RuntimeException("Exception during sleep", e);
        }
    }
}
