/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.tools;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A version of {@link Supplier} that allows throwing a checked {@link Exception}.
 * <p>
 * This interface is useful when working with functional programming constructs that need to
 * handle checked exceptions, such as stream operations or deferred computations.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ThrowingSupplier<String, IOException> fileReader = () -> {
 *     try (BufferedReader reader = new BufferedReader(new FileReader("example.txt"))) {
 *         return reader.readLine();
 *     }
 * };
 *
 * try {
 *     String line = fileReader.get();
 *     System.out.println("Read line: " + line);
 * } catch (IOException e) {
 *     e.printStackTrace();
 * }
 * }</pre>
 *
 * @param <T> The type of the result supplied
 * @param <E> The type of exception thrown by the supplier
 * @see Supplier
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

    T get() throws E;

    default T unsafeGet(){
        return Try.unsafe(this);
    }

    /**
     * @throws UncheckedException if given {@link ThrowingFunction} throws
     */
    static <T, E extends Exception> Supplier<T> toUnchecked(ThrowingSupplier<T, E> throwingFunction) throws UncheckedException {
        return silence(throwingFunction, e -> {
            throw UncheckedException.throwUncheckedException(e);
        });
    }

    static <T, E extends Exception> Supplier<T> silence(ThrowingSupplier<T, E> throwingFunction, Function<Exception, T> exceptionHandler) {
        return () -> {
            try {
                return throwingFunction.get();
            } catch (Exception e) {
                return exceptionHandler.apply(e);
            }
        };
    }
}
