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
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.BsonDocument;
import org.bson.Document;

public class MongoUpdateAction implements Action {

    private final MongoDatabaseFactory mongoDatabaseFactory = new DefaultMongoDatabaseFactory();
    private final Target target;
    private final Logger logger;
    private final String collection;
    private final String filter;
    private final String update;
    private final List<String> arrayFilters;

    public MongoUpdateAction(Target target,
                           Logger logger,
                           @Input("collection") String collection,
                           @Input("filter") String filter,
                           @Input("update") String update,
                           // See https://jira.mongodb.org/browse/SERVER-831 for usage.
                           // Only since @3.5.12 mongodb version
                           @Input("arrayFilters") List<String> arrayFilters) {
        this.target = target;
        this.logger = logger;
        this.collection = collection;
        this.filter = filter;
        this.update = update;
        this.arrayFilters = ofNullable(arrayFilters).orElse(emptyList());
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(collection, "collection"),
            notBlankStringValidation(update, "update"),
            mongoTargetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try (CloseableResource<MongoDatabase> database = mongoDatabaseFactory.create(target)) {
            MongoCollection<Document> collection = database
                .getResource()
                .getCollection(this.collection);

            final UpdateResult updateResult;
            if (!arrayFilters.isEmpty()) {
                List<BsonDocument> arrayFilterDocuments = arrayFilters.stream()
                    .map(BsonDocument::parse)
                    .collect(Collectors.toList());
                updateResult = collection
                    .updateMany(
                        BsonDocument.parse(filter),
                        BsonDocument.parse(update),
                        new UpdateOptions().arrayFilters(arrayFilterDocuments)
                    );
            } else {
                updateResult = collection
                    .updateMany(
                        BsonDocument.parse(filter),
                        BsonDocument.parse(update)
                    );
            }
            if (!updateResult.wasAcknowledged()) {
                logger.error("Update was not acknowledged");
                return ActionExecutionResult.ko();
            }
            long modifiedCount = updateResult.getModifiedCount();
            logger.info("Modified in Mongo collection '" + this.collection + "': " + modifiedCount + " documents");
            return ActionExecutionResult.ok(Collections.singletonMap("modifiedCount", modifiedCount));
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }
}
