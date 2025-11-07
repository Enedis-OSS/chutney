/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.domain;

import fr.enedis.chutney.index.api.dto.Hit;
import java.util.List;

public interface IndexRepository<T> {
    void save(T item);

    default void saveAll(List<T> items) {
        items.forEach(this::save);
    }

    void delete(String id);

    List<Hit> search(String keyword);

    IndexObject indexObject();
}
