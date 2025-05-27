/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

public class ChutneyMemoryInfo {

    private static final long MAX_MEMORY = Runtime.getRuntime().maxMemory(); // Fixed at startup

    public static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public static boolean hasEnoughAvailableMemory(int minimumMemoryPercentageRequired) {
        long minimumMemoryRequired = (MAX_MEMORY / 100) * minimumMemoryPercentageRequired;
        return availableMemory() > minimumMemoryRequired;
    }

    public static long maxMemory() {
        return MAX_MEMORY;
    }

    private static long availableMemory() {
        return MAX_MEMORY - usedMemory();
    }
}
