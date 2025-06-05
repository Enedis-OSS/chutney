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
        Path path = Paths.get(indexDir, indexName);
        analyzer = new CustemChutneyAnalyzer();
        indexDirectory = initFromPath(path);
        indexWriter = buildIndexWriter(path, indexName);
    }

    private Directory initFromPath(Path path) {
        try {
            initFolder(path);
            return FSDirectory.open(path);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open index directory", e);
        }
    }

    private IndexWriter buildIndexWriter(Path path, String indexName) {
        IndexWriter tmpIndexWriter;
        try {
            tmpIndexWriter = new IndexWriter(indexDirectory, getIndexWriterConfig());
            tmpIndexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't build index writer", e);
        } catch (IllegalArgumentException iae) {
            var suppressedCorruptIndexException = Arrays.stream(iae.getSuppressed())
                .filter(e -> e instanceof CorruptIndexException)
                .findAny();
            if (suppressedCorruptIndexException.isPresent()) {
                LOGGER.warn("Index corrupted... Clean index {} for re-indexing", indexName, suppressedCorruptIndexException.get());
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
        return tmpIndexWriter;
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
