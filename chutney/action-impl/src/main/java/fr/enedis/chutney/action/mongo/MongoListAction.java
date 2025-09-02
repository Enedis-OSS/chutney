/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import static fr.enedis.chutney.action.mongo.MongoActionValidatorsUtils.mongoTargetValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MongoListAction implements Action {

    private final MongoDatabaseFactory mongoDatabaseFactory = new DefaultMongoDatabaseFactory();
    private final Target target;
    private final Logger logger;

    public MongoListAction(Target target, Logger logger) {
        this.target = target;
        this.logger = logger;
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            mongoTargetValidation(target)
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try (CloseableResource<MongoDatabase> database = mongoDatabaseFactory.create(target)) {
            MongoIterable<String> collectionNames = database.getResource().listCollectionNames();
            var collectionNameList = new ArrayList<String>();
            collectionNames.iterator().forEachRemaining(collectionNameList::add);
            logger.info("Found " + collectionNameList.size() + " collection(s)");
            return ActionExecutionResult.ok(Collections.singletonMap("collectionNames", collectionNameList));
        } catch (IllegalArgumentException | MongoException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }
}
