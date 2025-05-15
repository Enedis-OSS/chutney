/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import static fr.enedis.chutney.action.mongo.MongoActionValidatorsUtils.mongoTargetValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.Document;

public class MongoFindAction implements Action {

    private final MongoDatabaseFactory mongoDatabaseFactory = new DefaultMongoDatabaseFactory();
    private final Target target;
    private final Logger logger;
    private final String collection;
    private final String query;
    private final Integer limit;

    public MongoFindAction(Target target,
                         Logger logger,
                         @Input("collection") String collection,
                         @Input("query") String query,
                         @Input("limit") Integer limit
    ) {
        this.target = target;
        this.logger = logger;
        this.collection = collection;
        this.query = query;
        this.limit = ofNullable(limit).orElse(20);
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(collection, "collection"),
            notBlankStringValidation(query, "query"),
            mongoTargetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try (CloseableResource<MongoDatabase> database = mongoDatabaseFactory.create(target)) {
            MongoIterable<String> documents = database.getResource()
                .getCollection(collection)
                .find(BsonDocument.parse(query))
                .limit(limit)
                .map(Document::toJson);

            var documentList = new ArrayList<String>();
            documents.iterator().forEachRemaining(documentList::add);
            logger.info("Found " + documentList.size() + " document(s)");
            return ActionExecutionResult.ok(Collections.singletonMap("documents", documentList));
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }
}
