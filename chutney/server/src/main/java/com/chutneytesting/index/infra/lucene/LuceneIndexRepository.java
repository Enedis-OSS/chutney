/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.index.infra.lucene;

import com.chutneytesting.index.infra.lucene.config.IndexConfig;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class LuceneIndexRepository {

    private final IndexWriter indexWriter;
    private final Directory indexDirectory;
    private final Analyzer analyzer;
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexRepository.class);


    public LuceneIndexRepository(IndexConfig config) {
        this.indexDirectory = config.directory();
        this.indexWriter = config.indexWriter();
        this.analyzer = config.analyzer();
    }

    public void index(Document document) {
        try {
            this.indexWriter.addDocument(document);
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't index data", e);
        }
    }

    public void update(Query query, Document document) {
        try {
            this.indexWriter.updateDocuments(query, List.of(document));
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't index data", e);
        }
    }

    public List<Document> search(Query query, int limit, Sort sort) {
        List<Document> result = new ArrayList<>();
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            ScoreDoc[] hits = searcher.search(query, limit, sort).scoreDocs;
            StoredFields storedFields = searcher.storedFields();
            for (ScoreDoc hit : hits){
                result.add(storedFields.document(hit.doc));
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    public int count(Query query) {
        int count = 0;
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            count  = searcher.count(query);

        } catch (IOException e) {
            throw new RuntimeException("Couldn't count elements in index", e);
        }
        return count;
    }

    public void delete(Query query) {
        try {
            indexWriter.deleteDocuments(query);
            indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't delete index using query " + query, e);
        }
    }

    public void deleteAll() {
        try {
            indexWriter.deleteAll();
            indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't delete all indexes", e);
        }
    }

    public String highlight(Query query, String field, String value, boolean strict) {
        QueryScorer scorer = new QueryScorer(query);
        Formatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
        Highlighter highlighter = new Highlighter(formatter, scorer);
        return highlight(highlighter, field, value, strict);
    }

    private String highlight(Highlighter highlighter, String fieldName, String text, boolean strict) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        try (TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(text))) {
            String highlightedText = highlighter.getBestFragment(tokenStream, text);
            return highlightedText != null ? highlightedText : strict ? null : text;
        } catch (IOException | InvalidTokenOffsetsException e) {
            LOGGER.warn("Unable to highlight {} field: {}", fieldName, e);
            return text;
        }
    }
}

