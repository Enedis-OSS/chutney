/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.durationValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.targetValidation;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.spi.validation.Validator;

public class HttpActionHelper {

    private HttpActionHelper() {
    }

    static Validator<?>[] httpCommonValidation(Target target, String timeout) {
        return new Validator[]{targetValidation(target),
            durationValidation(timeout, "timeout")};
    }
}
