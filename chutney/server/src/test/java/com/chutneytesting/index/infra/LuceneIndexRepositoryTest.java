/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.index.infra;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chutneytesting.index.infra.config.IndexConfig;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LuceneIndexRepositoryTest {

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private LuceneIndexRepository luceneIndexRepository;

    @BeforeEach
    public void setUp() throws IOException {
        directory = new ByteBuffersDirectory();
        analyzer = new CustemChutneyAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, config);

        IndexConfig indexConfig = mock(IndexConfig.class);
        when(indexConfig.directory()).thenReturn(directory);
        when(indexConfig.indexWriter()).thenReturn(indexWriter);
        when(indexConfig.analyzer()).thenReturn(analyzer);

        luceneIndexRepository = new LuceneIndexRepository(indexConfig);
    }

    @AfterEach
    public void tearDown() throws IOException {
        indexWriter.close();
        directory.close();
    }

    private Document createDocument(String title, String content) {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.YES));
        return doc;
    }


    @Test
    public void testIndexAndSearch() throws Exception {
        luceneIndexRepository.index(createDocument("Lucene Introduction", "Introduction to search engine"));
        luceneIndexRepository.index(createDocument("Advanced search", "Advanced topics in Lucene"));

        Query query = createQuery("content", "Lucene");
        List<Document> results = luceneIndexRepository.search(query, 10);

        assertEquals(1, results.size());
        assertTrue(results.stream().anyMatch(doc -> doc.get("title").equals("Advanced search")));
    }

    @Test
    public void testCount() throws Exception {
        luceneIndexRepository.index(createDocument("Test Document", "This is a test document"));

        Query query = createQuery("content", "test");
        int count = luceneIndexRepository.count(query);

        assertEquals(1, count);
    }

    @Test
    public void testDelete() throws Exception {
        luceneIndexRepository.index(createDocument("Delete Test", "Document to delete"));

        Query query = createQuery("title", "Delete Test");
        luceneIndexRepository.delete(query);

        List<Document> results = luceneIndexRepository.search(query, 10);
        assertTrue(results.isEmpty());
    }

    private Query createQuery(String field, String queryString) throws Exception {
        QueryParser parser = new QueryParser(field, analyzer);
        return parser.parse(queryString);
    }

    @Test
    public void testHighlight() throws Exception {
        String text = "Lucene is a powerful search engine library that supports full-text search ";
        List<String> keywords = List.of("sear", "librar");

        String highlightedText = luceneIndexRepository.highlight(keywords, "title", text, false);

        assertThat(highlightedText).contains("<mark>search</mark>");
        assertThat(highlightedText).contains("<mark>library</mark>");
    }

}
