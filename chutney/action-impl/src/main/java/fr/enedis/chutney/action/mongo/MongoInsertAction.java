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

import com.google.common.base.Ascii;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import java.util.List;
import org.bson.Document;

public class MongoInsertAction implements Action {

    private final MongoDatabaseFactory mongoDatabaseFactory = new DefaultMongoDatabaseFactory();
    private final Target target;
    private final Logger logger;
    private final String collection;
    private final String document;

    public MongoInsertAction(Target target,
                           Logger logger,
                           @Input("collection") String collection,
                           @Input("document") String document) {
        this.target = target;
        this.logger = logger;
        this.collection = collection;
        this.document = document;
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(collection, "collection"),
            notBlankStringValidation(document, "document"),
            mongoTargetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try (CloseableResource<MongoDatabase> database = mongoDatabaseFactory.create(target)) {
            database.getResource().getCollection(collection).insertOne(Document.parse(document));
            logger.info(
                "Inserted in Mongo collection '" + collection + "':\n\t" +
                    Ascii.truncate(document.replace("\n", "\n\t"), 50, "...")
            );
            return ActionExecutionResult.ok();
        } catch (IllegalArgumentException | MongoException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }
}
