/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.dataset.infra.index;

import static org.apache.lucene.search.BooleanClause.Occur;

import com.chutneytesting.index.api.dto.Hit;
import com.chutneytesting.index.domain.IndexRepository;
import com.chutneytesting.index.infra.lucene.LuceneIndexRepository;
import com.chutneytesting.scenario.infra.raw.TagListMapper;
import com.chutneytesting.server.core.domain.dataset.DataSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class DatasetIndexRepository implements IndexRepository<DataSet> {
    private final String WHAT_VALUE = "dataset";
    private final String WHAT = "what";
    private final String ID = "id";
    private final String NAME = "name";
    private final String DESCRIPTION = "description";
    private final String CONTENT = "content";
    private final String TAGS = "tags";
    private final LuceneIndexRepository luceneIndexRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetIndexRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules();


    public DatasetIndexRepository(LuceneIndexRepository luceneIndexRepository) {
        this.luceneIndexRepository = luceneIndexRepository;
    }

    @Override
    public void save(DataSet dataSet) {
        Document document = new Document();
        document.add(new StringField(WHAT, WHAT_VALUE, Field.Store.YES));
        document.add(new TextField(ID, dataSet.id, Field.Store.YES));
        document.add(new TextField(NAME, dataSet.name.toLowerCase(), Field.Store.YES));
        document.add(new TextField(DESCRIPTION, dataSet.description.toLowerCase(), Field.Store.YES));
        document.add(new TextField(CONTENT, content(dataSet), Field.Store.YES));
        document.add(new TextField(TAGS, TagListMapper.tagsToString(dataSet.tags).toLowerCase(), Field.Store.YES));
        // for sorting
        document.add(new SortedDocValuesField(ID, new BytesRef(dataSet.id.getBytes())));
        luceneIndexRepository.update(byIdQuery(dataSet.id), document);
    }

    private String content(DataSet dataSet) {
        try {
            return objectMapper.writeValueAsString(Map.of("constant", dataSet.constants, "datatable", dataSet.datatable));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String id) {
        Query query = byIdQuery(id);
        luceneIndexRepository.delete(query);
    }

    @Override
    public List<Hit> search(String keyword) {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        BooleanQuery propertiesQuery;
        propertiesQuery = new BooleanQuery.Builder()
            .setMinimumNumberShouldMatch(1)
            .add(likeQuery(ID, keyword), Occur.SHOULD)
            .add(likeQuery(NAME, keyword), Occur.SHOULD)
            .add(likeQuery(DESCRIPTION, keyword), Occur.SHOULD)
            .add(likeQuery(CONTENT, keyword), Occur.SHOULD)
            .add(likeQuery(TAGS, keyword), Occur.SHOULD)
            .build();

        BooleanQuery query = new BooleanQuery.Builder()
            .add(whatQuery, Occur.MUST)
            .add(propertiesQuery, Occur.MUST)
            .build();

        Sort sort = new Sort(SortField.FIELD_SCORE, new SortField(ID, SortField.Type.STRING, true));

        List<Document> hits = luceneIndexRepository.search(query, 100, sort);

        return hits
            .stream()
            .map(doc -> highlight(doc, keyword))
            .toList();
    }

    public int count() {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        return luceneIndexRepository.count(whatQuery);
    }

    private WildcardQuery likeQuery(String column, String keyword) {
        return new WildcardQuery(new Term(column, "*" + keyword.toLowerCase() + "*"));
    }

    private Query byIdQuery(String id) {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        Query idQuery = new TermQuery(new Term(ID, id));
        return new BooleanQuery.Builder()
            .add(idQuery, Occur.MUST)
            .add(whatQuery, Occur.MUST)
            .build();
    }

    private Hit highlight(Document doc, String keyword) {
        String highlightedId = luceneIndexRepository.highlight(likeQuery(ID, keyword), ID, doc.get(ID), false);
        String highlightedTitle = luceneIndexRepository.highlight(likeQuery(NAME, keyword), NAME, doc.get(NAME), false);
        String highlightedDescription = luceneIndexRepository.highlight(likeQuery(DESCRIPTION, keyword), DESCRIPTION, doc.get(DESCRIPTION), false);
        String highlightedContent = luceneIndexRepository.highlight(likeQuery(CONTENT, keyword), CONTENT, doc.get(CONTENT), false);
        List<String> highlightedTags = TagListMapper.tagsStringToList(doc.get(TAGS)).stream()
            .map(tag -> luceneIndexRepository.highlight(likeQuery(TAGS, keyword), TAGS, tag, false))
            .filter(StringUtils::isNotBlank)
            .toList();

        return new Hit(
            highlightedId,
            highlightedTitle,
            highlightedDescription,
            highlightedContent,
            highlightedTags,
            WHAT_VALUE
        );
    }


}
