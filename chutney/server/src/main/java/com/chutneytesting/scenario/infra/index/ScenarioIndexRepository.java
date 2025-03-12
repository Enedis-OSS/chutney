/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.scenario.infra.index;

import com.chutneytesting.index.domain.AbstractIndexRepository;
import com.chutneytesting.index.infra.LuceneIndexRepository;
import com.chutneytesting.scenario.infra.jpa.ScenarioEntity;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class ScenarioIndexRepository extends AbstractIndexRepository<ScenarioEntity> {

    public ScenarioIndexRepository(@Qualifier("scenarioLuceneIndexRepository") LuceneIndexRepository luceneIndexRepository) {
        super("scenario", luceneIndexRepository);
    }

    @Override
    protected Document createDocument(ScenarioEntity scenario) {
        Document document = new Document();
        document.add(new StringField(WHAT, whatValue, Field.Store.YES));
        document.add(new StringField(ID, scenario.getId().toString(), Field.Store.YES));
        document.add(new TextField(TITLE, scenario.getTitle(), Field.Store.YES));
        document.add(new TextField(DESCRIPTION, scenario.getDescription(), Field.Store.YES));
        document.add(new TextField(CONTENT, scenario.getContent(), Field.Store.YES));
        document.add(new TextField(TAGS, scenario.getTags(), Field.Store.YES));
        return document;
    }

    @Override
    protected String getId(ScenarioEntity scenario) {
        return scenario.getId().toString();
    }
}
