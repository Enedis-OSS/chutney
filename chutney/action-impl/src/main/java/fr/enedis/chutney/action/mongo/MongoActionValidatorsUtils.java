/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.spi.validation.Validator;
import org.apache.commons.lang3.StringUtils;

public class MongoActionValidatorsUtils {

    public static Validator<Target> mongoTargetValidation(Target target) {
        return targetValidation(target)
            .validate(t -> target.property("databaseName").orElse(null), StringUtils::isNotBlank, "Missing Target property 'databaseName'");
    }
}
