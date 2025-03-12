/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.index.infra;

import com.chutneytesting.index.infra.config.IndexConfig;
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
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        } catch (Exception e) {
            LOGGER.error("Couldn't index data", e);
        }
    }

    public void update(Query query, Document document) {
        try {
            this.indexWriter.updateDocuments(query, List.of(document));
            this.indexWriter.commit();
        } catch (Exception e) {
            LOGGER.error("Couldn't index data", e);
        }
    }

    public List<Document> search(Query query, int limit) {
        List<Document> result = new ArrayList<>();
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            ScoreDoc[] hits = searcher.search(query, limit).scoreDocs;
            StoredFields storedFields = searcher.storedFields();
            for (ScoreDoc hit : hits) {
                result.add(storedFields.document(hit.doc));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public int count(Query query) {
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            return searcher.count(query);

        } catch (Exception e) {
            LOGGER.error("Couldn't count elements in index", e);
            return 0;
        }
    }

    public void delete(Query query) {
        try {
            indexWriter.deleteDocuments(query);
            indexWriter.commit();
        } catch (IOException e) {
            LOGGER.error("Couldn't delete index using query " + query, e);
        }
    }

    public void deleteAll() {
        try {
            indexWriter.deleteAll();
            indexWriter.commit();
        } catch (Exception e) {
            LOGGER.error("Couldn't delete all indexes", e);
        }
    }

    public String highlight(List<String> keywords, String field, String value, boolean strict) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        Query query = createCombinedWildcardQuery(keywords, field);
        Highlighter highlighter = createHighlighter(query);
        return processHighlight(highlighter, field, value, strict);
    }

    private Query createCombinedWildcardQuery(List<String> keywords, String field) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String keyword : keywords) {
            builder.add(new WildcardQuery(new Term(field, "*" + QueryParser.escape(keyword) + "*")), BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    private Highlighter createHighlighter(Query query) {
        QueryScorer scorer = new QueryScorer(query);
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
        return new Highlighter(formatter, scorer);
    }

    private String processHighlight(Highlighter highlighter, String fieldName, String text, boolean strict) {
        try (TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(text))) {
            String highlightedText = highlighter.getBestFragment(tokenStream, text);
            return highlightedText != null ? highlightedText : (strict ? null : text);
        } catch (IOException | InvalidTokenOffsetsException e) {
            LOGGER.warn("Unable to highlight {} field: {}", fieldName, e);
            return text;
        } catch (RuntimeException e) {
            LOGGER.error("processHighlight error",e);
            return "";
        }
    }
}

