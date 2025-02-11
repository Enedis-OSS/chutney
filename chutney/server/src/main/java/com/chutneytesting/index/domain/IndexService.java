/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.index.domain;

import com.chutneytesting.index.api.dto.Hit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;

@Service
public class IndexService {
    private final List<IndexRepository<?>> indexRepositories;

    public IndexService(List<IndexRepository<?>> indexRepositories) {
        this.indexRepositories = indexRepositories;
    }

    public List<Hit> search(String query) {
        List<CompletableFuture<List<Hit>>> futures = indexRepositories.stream()
            .map(repo -> CompletableFuture.supplyAsync(() -> repo.search(query)))
            .toList();

        CompletableFuture<Void> allSearches = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allSearches.thenApply(v -> futures.stream()
                .flatMap(future -> {
                    try {
                        return future.get().stream();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList())
            .join();
    }
}
