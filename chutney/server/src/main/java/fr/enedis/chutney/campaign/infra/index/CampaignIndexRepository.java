/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra.index;

import fr.enedis.chutney.campaign.infra.jpa.CampaignEntity;
import fr.enedis.chutney.index.domain.AbstractIndexRepository;
import fr.enedis.chutney.index.infra.LuceneIndexRepository;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class CampaignIndexRepository extends AbstractIndexRepository<CampaignEntity> {

    public CampaignIndexRepository(@Qualifier("campaignLuceneIndexRepository")  LuceneIndexRepository luceneIndexRepository) {
        super("campaign", luceneIndexRepository);
    }

    @Override
    protected Document createDocument(CampaignEntity campaign) {
        Document document = new Document();
        document.add(new StringField(WHAT, whatValue, Field.Store.YES));
        document.add(new StringField(ID, campaign.id().toString(), Field.Store.YES));
        document.add(new TextField(TITLE, campaign.title(), Field.Store.YES));
        document.add(new TextField(DESCRIPTION, campaign.description(), Field.Store.YES));
        document.add(new TextField(TAGS, campaign.tags(), Field.Store.YES));
        return document;
    }

    @Override
    protected String getId(CampaignEntity campaign) {
        return campaign.id().toString();
    }
}
