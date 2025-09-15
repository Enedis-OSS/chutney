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

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import java.util.Collections;
import java.util.List;
import org.bson.BsonDocument;

public class MongoDeleteAction implements Action {

    private final MongoDatabaseFactory mongoDatabaseFactory = new DefaultMongoDatabaseFactory();
    private final Target target;
    private final Logger logger;
    private final String collection;
    private final String query;

    public MongoDeleteAction(Target target,
                           Logger logger,
                           @Input("collection") String collection,
                           @Input("query") String query) {
        this.target = target;
        this.logger = logger;
        this.collection = collection;
        this.query = query;
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
            DeleteResult deleteResult = database.getResource().getCollection(collection).deleteMany(BsonDocument.parse(query));
            if (!deleteResult.wasAcknowledged()) {
                logger.error("Deletion was not acknowledged");
                return ActionExecutionResult.ko();
            }
            long deletedCount = deleteResult.getDeletedCount();
            logger.info("Deleted " + deletedCount + " document(s)");
            return ActionExecutionResult.ok(Collections.singletonMap("deletedCount", deletedCount));
        } catch (IllegalArgumentException | MongoException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }
}
