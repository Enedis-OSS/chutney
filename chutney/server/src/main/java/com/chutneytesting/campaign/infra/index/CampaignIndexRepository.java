/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.campaign.infra.index;

import static org.apache.lucene.search.BooleanClause.Occur;

import com.chutneytesting.campaign.infra.jpa.CampaignEntity;
import com.chutneytesting.index.api.dto.Hit;
import com.chutneytesting.index.domain.IndexRepository;
import com.chutneytesting.index.infra.lucene.LuceneIndexRepository;
import com.chutneytesting.scenario.infra.raw.TagListMapper;
import java.util.List;
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
import org.springframework.stereotype.Repository;

@Repository
public class CampaignIndexRepository implements IndexRepository<CampaignEntity> {
    private final String WHAT_VALUE = "campaign";
    private final String WHAT = "what";
    private final String ID = "id";
    private final String TITLE = "title";
    private final String DESCRIPTION = "description";
    private final String TAGS = "tags";
    private final LuceneIndexRepository luceneIndexRepository;

    public CampaignIndexRepository(LuceneIndexRepository luceneIndexRepository) {
        this.luceneIndexRepository = luceneIndexRepository;
    }

    @Override
    public void save(CampaignEntity campaign) {
        Document document = new Document();
        document.add(new StringField(WHAT, WHAT_VALUE, Field.Store.YES));
        document.add(new TextField(ID, campaign.id().toString(), Field.Store.YES));
        document.add(new TextField(TITLE, campaign.title().toLowerCase(), Field.Store.YES));
        document.add(new TextField(DESCRIPTION, campaign.description().toLowerCase(), Field.Store.YES));
        document.add(new TextField(TAGS, campaign.tags().toLowerCase(), Field.Store.YES));
        // for sorting
        document.add(new SortedDocValuesField(ID, new BytesRef(campaign.id().toString().getBytes()) ));
        luceneIndexRepository.update(byIdQuery(campaign.id().toString()), document);
    }

    @Override
    public void delete(String id) {
        BooleanQuery query = byIdQuery(id);
        luceneIndexRepository.delete(query);
    }

    @Override
    public List<Hit> search(String keyword) {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        BooleanQuery propertiesQuery;
        propertiesQuery = new BooleanQuery.Builder()
            .setMinimumNumberShouldMatch(1)
            .add(likeQuery(ID, keyword), Occur.SHOULD)
            .add(likeQuery(TITLE, keyword), Occur.SHOULD)
            .add(likeQuery(DESCRIPTION, keyword), Occur.SHOULD)
            .add(likeQuery(TAGS, keyword), Occur.SHOULD)
            .build();

        BooleanQuery query = new BooleanQuery.Builder()
            .add(whatQuery, Occur.MUST)
            .add(propertiesQuery, Occur.MUST)
            .build();

        Sort sort = new Sort(SortField.FIELD_SCORE, new SortField(ID, SortField.Type.STRING, true));

        return luceneIndexRepository.search(query, 100, sort)
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


    private BooleanQuery byIdQuery(String id) {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        Query idQuery = new TermQuery(new Term(ID, id));
        return new BooleanQuery.Builder()
            .add(idQuery, Occur.MUST)
            .add(whatQuery, Occur.MUST)
            .build();
    }

    private Hit highlight(Document doc, String keyword) {
        String highlightedId = luceneIndexRepository.highlight(likeQuery(ID, keyword), ID, doc.get(ID), false);
        String highlightedTitle = luceneIndexRepository.highlight(likeQuery(TITLE, keyword), TITLE, doc.get(TITLE), false);
        String highlightedDescription = luceneIndexRepository.highlight(likeQuery(DESCRIPTION, keyword), DESCRIPTION, doc.get(DESCRIPTION), false);
        List<String> highlightedTags = TagListMapper.tagsStringToList(doc.get(TAGS)).stream()
            .map(tag -> luceneIndexRepository.highlight(likeQuery(TAGS, keyword), TAGS, tag, true))
            .filter(StringUtils::isNotBlank)
            .toList();

        return new Hit(
            highlightedId,
            highlightedTitle,
            highlightedDescription,
            null,
            highlightedTags,
            WHAT_VALUE
        );
    }
}
