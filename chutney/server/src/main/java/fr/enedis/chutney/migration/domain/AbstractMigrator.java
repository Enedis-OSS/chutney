/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package fr.enedis.chutney.migration.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public abstract class AbstractMigrator<T> implements DataMigrator {
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Override
    public void migrate() {
        if (isMigrationDone()) {
            LOGGER.info("{} index not empty. Skipping indexing...", getEntityName());
            return;
        }
        LOGGER.info("Start indexing {}...", getEntityName());
        PageRequest firstPage = PageRequest.of(0, 10);
        int count = 0;
        migrate(firstPage, count);
    }

    protected void migrate(Pageable pageable, int previousCount) {
        LOGGER.debug("Indexing page n° {}", pageable.getPageNumber());
        Slice<T> slice = findAll(pageable);
        List<T> entities = slice.getContent();
        index(entities);
        int count = previousCount + slice.getNumberOfElements();
        if (slice.hasNext()) {
            migrate(slice.nextPageable(), count);
        } else {
            LOGGER.info("{} {} successfully indexed", count, getEntityName());
        }
    }

    protected abstract Slice<T> findAll(Pageable pageable);

    protected abstract void index(List<T> entities);

    protected abstract boolean isMigrationDone();

    protected abstract String getEntityName();
}
