/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.dataset.infra.index;

import com.chutneytesting.index.domain.AbstractIndexRepository;
import com.chutneytesting.index.infra.LuceneIndexRepository;
import com.chutneytesting.scenario.infra.raw.TagListMapper;
import com.chutneytesting.server.core.domain.dataset.DataSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.springframework.stereotype.Repository;

@Repository
public class DatasetIndexRepository extends AbstractIndexRepository<DataSet> {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public DatasetIndexRepository(LuceneIndexRepository luceneIndexRepository) {
        super("dataset", luceneIndexRepository);
    }

    @Override
    protected Document createDocument(DataSet dataSet) {
        Document document = new Document();
        document.add(new StringField(WHAT, whatValue, Field.Store.YES));
        document.add(new TextField(ID, dataSet.id, Field.Store.YES));
        document.add(new TextField(TITLE, dataSet.name, Field.Store.YES));
        document.add(new TextField(DESCRIPTION, dataSet.description, Field.Store.YES));
        document.add(new TextField(CONTENT, content(dataSet), Field.Store.YES));
        document.add(new TextField(TAGS, TagListMapper.tagsToString(dataSet.tags), Field.Store.YES));
        return document;
    }

    @Override
    protected String getId(DataSet dataSet) {
        return dataSet.id;
    }

    private String content(DataSet dataSet) {
        try {
            return objectMapper.writeValueAsString(Map.of("constant", dataSet.constants, "datatable", dataSet.datatable));
        } catch (JsonProcessingException e) {
            String message = "Cannot serialize dataset: " + e.getMessage();
            LOGGER.error(message);
            return message;
        }
    }


}
