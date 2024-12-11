/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.index.domain;

import com.chutneytesting.index.api.dto.Hit;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IndexService {
    private final List<IndexRepository> indexRepositories;

    public IndexService(List<IndexRepository> indexRepositories) {
        this.indexRepositories = indexRepositories;
    }

    public List<Hit> search(String query) {
        return indexRepositories.stream()
            .map(repo -> repo.search(query))
            .flatMap(Collection::stream).toList();
    }
}
