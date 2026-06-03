/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
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
import org.apache.lucene.index.IndexFormatTooOldException;
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
        try {
            initFolder(path);
            OpenedIndex openedIndex = openIndex(path, indexName);
            indexDirectory = openedIndex.directory();
            indexWriter = openedIndex.writer();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open index directory", e);
        }
    }

    private OpenedIndex openIndex(Path path, String indexName) throws IOException {
        Directory directory = FSDirectory.open(path);
        try {
            return new OpenedIndex(directory, createAndCommitWriter(directory));
        } catch (IOException | IllegalArgumentException e) {
            closeDirectory(directory);
            if (!isRecoverableIndexError(e)) {
                if (e instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("Couldn't build index writer", e);
            }
            LOGGER.warn(
                "Index {} at {} is incompatible or corrupted. Recreating it for re-indexing.",
                indexName,
                path,
                e
            );
            cleanFolder(path);
            Directory recreatedDirectory = FSDirectory.open(path);
            return new OpenedIndex(recreatedDirectory, createAndCommitWriter(recreatedDirectory));
        }
    }

    private IndexWriter createAndCommitWriter(Directory directory) throws IOException {
        IndexWriter writer = new IndexWriter(directory, getIndexWriterConfig());
        writer.commit();
        return writer;
    }

    private static boolean isRecoverableIndexError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CorruptIndexException || current instanceof IndexFormatTooOldException) {
                return true;
            }
            current = current.getCause();
        }
        if (throwable instanceof IllegalArgumentException illegalArgumentException) {
            return Arrays.stream(illegalArgumentException.getSuppressed()).anyMatch(OnDiskIndexConfig::isRecoverableIndexError);
        }
        return false;
    }

    private static void closeDirectory(Directory directory) {
        try {
            directory.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close index directory before recreation", e);
        }
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

    private record OpenedIndex(Directory directory, IndexWriter writer) {
    }
}
