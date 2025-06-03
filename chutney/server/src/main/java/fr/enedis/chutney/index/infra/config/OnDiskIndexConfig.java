/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.infra.config;

import static fr.enedis.chutney.tools.file.FileUtils.cleanFolder;
import static fr.enedis.chutney.tools.file.FileUtils.initFolder;

import fr.enedis.chutney.index.infra.CustemChutneyAnalyzer;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnDiskIndexConfig implements IndexConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnDiskIndexConfig.class);

    private final IndexWriter indexWriter;
    private final Directory indexDirectory;
    private final Analyzer analyzer;

    public OnDiskIndexConfig(String indexDir, String indexName) {
        Path path;
        try {
            path = Paths.get(indexDir, indexName);
            initFolder(path);
            indexDirectory = FSDirectory.open(path);
            analyzer = new CustemChutneyAnalyzer();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open index directory", e);
        }

        IndexWriter tmpIndexWriter;
        try {
            tmpIndexWriter = new IndexWriter(indexDirectory, getIndexWriterConfig());
            tmpIndexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't build index writer", e);
        } catch (IllegalArgumentException iae) {
            var cie = Arrays.stream(iae.getSuppressed())
                .filter(e -> e instanceof CorruptIndexException)
                .findAny();
            if (cie.isPresent()) {
                LOGGER.warn("Index corrupted... Clean index {} for re-indexing", indexName, cie.get());
                cleanFolder(path);
                try {
                    tmpIndexWriter = new IndexWriter(indexDirectory, getIndexWriterConfig());
                    tmpIndexWriter.commit();
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't build index writer", e);
                }
            } else {
                throw new RuntimeException("Couldn't build index writer", iae);
            }
        }
        indexWriter = tmpIndexWriter;
    }

    private IndexWriterConfig getIndexWriterConfig() {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setRAMBufferSizeMB(64); // Default 16
        config.setMergePolicy(new TieredMergePolicy());
        return config;
    }

    @Override
    public Directory directory() {
        return indexDirectory;
    }

    @Override
    public IndexWriter indexWriter() {
        return indexWriter;
    }

    @Override
    public Analyzer analyzer() {
        return analyzer;
    }
}
