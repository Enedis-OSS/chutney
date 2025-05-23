/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

public interface CloseableResource<T> extends AutoCloseable {

	void close();

	T getResource();

	static <T> CloseableResource<T> build(T resource, Runnable closer) {
		return new CloseableResource<>() {

            @Override
            public void close() {
                closer.run();
            }

            @Override
            public T getResource() {
                return resource;
            }
        };
	}
}
