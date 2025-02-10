/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.index.infra.lucene.config;

import static com.chutneytesting.tools.file.FileUtils.initFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OnDiskIndexConfig implements IndexConfig {
    private final IndexWriter indexWriter;
    private final Directory indexDirectory;
    private final StandardAnalyzer analyzer;

    public OnDiskIndexConfig(@Value("${chutney.index-folder:~/.chutney/index}") String indexDir) {
        try {
            Path path = Paths.get(indexDir);
            initFolder(path);
            this.indexDirectory = FSDirectory.open(path);
            analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setRAMBufferSizeMB(64); // Default 16
            config.setMergePolicy(new TieredMergePolicy());
            this.indexWriter = new IndexWriter(indexDirectory, config);
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open index directory", e);
        }
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
