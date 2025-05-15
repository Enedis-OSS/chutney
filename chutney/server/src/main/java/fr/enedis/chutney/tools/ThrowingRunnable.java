/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

/**
 * A functional interface representing a {@link Runnable} that can throw an {@link Exception}.
 * <p>
 * It allows wrapping a runnable that may throw a checked exception, making it compatible
 * with functional programming constructs while preserving exception handling.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * {@code
 * ThrowingRunnable throwingRunnable = () -> {
 *     if (Math.random() > 0.5) {
 *         throw new IOException("Random failure");
 *     }
 *     System.out.println("Executed successfully");
 * };
 *
 * try {
 *     throwingRunnable.run();
 * } catch (Exception e) {
 *     e.printStackTrace();
 * }
 * }</pre>
 *
 * @see ThrowingSupplier
 */
@FunctionalInterface
public interface ThrowingRunnable extends ThrowingSupplier<Void, Exception> {
    void run() throws Exception;

    @Override
    default Void get() throws Exception {
        run();
        return null;
    }
}
