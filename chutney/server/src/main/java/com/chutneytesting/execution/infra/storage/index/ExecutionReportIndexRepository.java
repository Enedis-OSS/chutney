/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.execution.infra.storage.index;

import static org.apache.lucene.document.Field.Store;

import com.chutneytesting.execution.infra.storage.jpa.ScenarioExecutionReportEntity;
import com.chutneytesting.index.api.dto.Hit;
import com.chutneytesting.index.infra.lucene.LuceneIndexRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Repository;

@Repository
public class ExecutionReportIndexRepository {

    private final String WHAT_VALUE = "executionReport";
    private final String WHAT = "what";
    private final String ID = "id";
    private final String REPORT = "report";
    private final LuceneIndexRepository luceneIndexRepository;

    public ExecutionReportIndexRepository(LuceneIndexRepository luceneIndexRepository) {
        this.luceneIndexRepository = luceneIndexRepository;
    }

    public void save(ScenarioExecutionReportEntity report) {
        Document document = new Document();
        document.add(new StringField(WHAT, WHAT_VALUE, Store.YES));
        document.add(new StringField(ID, report.scenarioExecutionId().toString(), Store.YES));
        document.add(new TextField(REPORT, report.getReport().toLowerCase(), Store.NO));
        // for sorting
        document.add(new SortedDocValuesField(ID, new BytesRef(report.scenarioExecutionId().toString().getBytes())));
        luceneIndexRepository.index(document);
    }

    public void saveAll(List<ScenarioExecutionReportEntity> reports) {
        reports.forEach(this::save);
    }

    public void delete(Long scenarioExecutionId) {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        Query idQuery = new TermQuery(new Term(ID, scenarioExecutionId.toString()));
        BooleanQuery query = new BooleanQuery.Builder()
            .add(idQuery, BooleanClause.Occur.MUST)
            .add(whatQuery, BooleanClause.Occur.MUST)
            .build();
        luceneIndexRepository.delete(query);
    }

    public void deleteAllById(Set<Long> scenarioExecutionIds) {
        scenarioExecutionIds.forEach(this::delete);
    }


    public List<Long> idsByKeywordInReport(String keyword) {
        return search(keyword).stream()
            .map(Hit::id)
            .map(Long::parseLong)
            .toList();

    }

    private List<Hit> search(String keyword) {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        Query reportQuery = new WildcardQuery(new Term(REPORT, "*" + keyword.toLowerCase() + "*"));

        BooleanQuery query = new BooleanQuery.Builder()
            .add(reportQuery, BooleanClause.Occur.MUST)
            .add(whatQuery, BooleanClause.Occur.MUST)
            .build();

        return luceneIndexRepository.search(query, 100)
            .stream()
            .map(doc -> new Hit(doc.get(ID),
                null,
                null,
                null,
                Collections.emptyList(),
                WHAT_VALUE))
            .toList();
    }

    public int count() {
        Query whatQuery = new TermQuery(new Term(WHAT, WHAT_VALUE));
        return luceneIndexRepository.count(whatQuery);
    }
}
