/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package fr.enedis.chutney.index.domain;


import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import fr.enedis.chutney.index.api.dto.Hit;
import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import fr.enedis.chutney.scenario.infra.raw.TagListMapper;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public abstract class AbstractIndexRepository<T> implements IndexRepository<T> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndexRepository.class);
    protected static final String WHAT = "what";
    protected static final String ID = "id";
    protected static final String TITLE = "title";
    protected static final String CONTENT = "content";
    protected static final String DESCRIPTION = "description";
    protected static final String TAGS = "tags";

    protected final String whatValue;

    private final LuceneIndexRepository luceneIndexRepository;

    protected abstract Document createDocument(T entity);

    protected abstract String getId(T entity);

    protected AbstractIndexRepository(String whatValue, LuceneIndexRepository luceneIndexRepository) {
        this.whatValue = whatValue;
        this.luceneIndexRepository = luceneIndexRepository;
    }

    @Override
    public void save(T entity) {
        Document document = createDocument(entity);
        luceneIndexRepository.update(byIdQuery(getId(entity)), document);
    }

    @Override
    public void delete(String id) {
        Query query = byIdQuery(id);
        luceneIndexRepository.delete(query);
    }

    @Override
    public List<Hit> search(String keyword) {
        Query whatQuery = new TermQuery(new Term(WHAT, whatValue));

        String[] keywords = keyword.toLowerCase().split("\\s+");

        BooleanQuery.Builder propertiesQueryBuilder = new BooleanQuery.Builder();

        for (String kw : keywords) {
            BooleanQuery.Builder fieldQueryBuilder = new BooleanQuery.Builder()
                .setMinimumNumberShouldMatch(1)
                .add(likeQuery(ID, kw), SHOULD)
                .add(likeQuery(TITLE, kw), SHOULD)
                .add(likeQuery(DESCRIPTION, kw), SHOULD)
                .add(likeQuery(CONTENT, kw), SHOULD)
                .add(likeQuery(TAGS, kw), SHOULD);

            propertiesQueryBuilder.add(fieldQueryBuilder.build(), MUST);
        }

        BooleanQuery propertiesQuery = propertiesQueryBuilder.build();

        BooleanQuery query = new BooleanQuery.Builder()
            .add(whatQuery, MUST)
            .add(propertiesQuery, MUST)
            .build();

        List<Document> documents = luceneIndexRepository.search(query, 100);

        return documents
            .stream()
            .map(doc -> highlight(doc, Arrays.asList(keywords)))
            .toList();
    }

    public int count() {
        Query whatQuery = new TermQuery(new Term(WHAT, whatValue));
        return luceneIndexRepository.count(whatQuery);
    }

    private Query likeQuery(String column, String keyword) {
        return new WildcardQuery(new Term(column, "*" + keyword + "*"));
    }

    private Query byIdQuery(String id) {
        Query whatQuery = new TermQuery(new Term(WHAT, whatValue));
        Query idQuery = new TermQuery(new Term(ID, id));
        return new BooleanQuery.Builder()
            .add(idQuery, MUST)
            .add(whatQuery, MUST)
            .build();
    }

    public Hit highlight(Document doc, List<String> keywords) {

        String highlightedId = luceneIndexRepository.highlight(keywords, ID, doc.get(ID), false);
        String highlightedTitle = luceneIndexRepository.highlight(keywords, TITLE, doc.get(TITLE), false);
        String highlightedDescription = luceneIndexRepository.highlight(keywords, DESCRIPTION, doc.get(DESCRIPTION), false);
        String highlightedContent = luceneIndexRepository.highlight(keywords, CONTENT, doc.get(CONTENT), false);
        List<String> highlightedTags = TagListMapper.tagsStringToList(doc.get(TAGS)).stream()
            .map(tag -> luceneIndexRepository.highlight(keywords, TAGS, tag, false))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());

        return new Hit(
            highlightedId,
            highlightedTitle,
            highlightedDescription,
            highlightedContent,
            highlightedTags,
            whatValue
        );
    }
}
