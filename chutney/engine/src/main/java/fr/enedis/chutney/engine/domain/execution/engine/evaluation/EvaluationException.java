/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.evaluation;

import org.springframework.expression.ParseException;

@SuppressWarnings("serial")
public class EvaluationException extends RuntimeException {

    EvaluationException(String expression) {
        super("Cannot resolve " + expression + ", Spring evaluation is null");
    }

    EvaluationException(String expression, Exception e) {
        super("Cannot resolve " + expression + " , " + e.getMessage(), e);
    }

    EvaluationException(String expression, ParseException e) {
        super("Cannot parse " + expression + " , " + e.getMessage(), e);
    }
}
