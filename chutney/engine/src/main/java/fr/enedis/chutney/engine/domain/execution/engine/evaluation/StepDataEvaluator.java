/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.evaluation;

import static fr.enedis.chutney.engine.domain.execution.engine.evaluation.Strings.escapeForRegex;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.evaluation.SpelFunctions;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class StepDataEvaluator {

    private static final String EVALUATION_STRING_PREFIX = "${";
    private static final String EVALUATION_STRING_SUFFIX = "}";
    private static final String EVALUATION_STRING_ESCAPE = "\\";
    private static final Pattern EVALUATION_OBJECT_PATTERN = Pattern.compile("^(?:" + escapeForRegex(EVALUATION_STRING_ESCAPE) + ")?" + escapeForRegex(EVALUATION_STRING_PREFIX) + "(?:(?!" + escapeForRegex(EVALUATION_STRING_PREFIX) + ").)*" + escapeForRegex(EVALUATION_STRING_SUFFIX) + "$", Pattern.DOTALL);


    private final SpelFunctions spelFunctions;
    private final ExpressionParser parser = new SpelExpressionParser();

    public StepDataEvaluator(SpelFunctions spelFunctions) {
        this.spelFunctions = spelFunctions;
    }

    public Map<String, Object> evaluateNamedDataWithContextVariables(final Map<String, Object> data, final Map<String, Object> contextVariables) {
        Map<String, Object> evaluatedNamedData = new LinkedHashMap<>();

        StandardEvaluationContext evaluationContext = buildEvaluationContext(contextVariables);

        data.forEach(
            (dataName, dataValue) -> {
                Object value = evaluateObject(dataValue, evaluationContext);
                evaluatedNamedData.put(dataName, value);
                evaluationContext.setVariable(dataName, value);
            }
        );
        return evaluatedNamedData;
    }

    public Object evaluate(final Object o, final Map<String, Object> contextVariables) {
        return evaluate(o, contextVariables, false);
    }

    public String evaluateString(final String s, final Map<String, Object> contextVariables) {
        return (String) this.evaluate(s, contextVariables);
    }

    public String silentEvaluateString(final String s, final Map<String, Object> contextVariables) {
        return (String) this.evaluate(s, contextVariables, true);
    }

    public Target evaluateTarget(final Target target, final Map<String, Object> contextVariables) {
        TargetImpl.TargetBuilder builder = TargetImpl.builder();

        StandardEvaluationContext evaluationContext = buildEvaluationContext(contextVariables);

        builder.withName(target.name());
        builder.withUrl((String) evaluateObject(target.rawUri(), evaluationContext));
        builder.withProperties((Map<String, String>) evaluateObject(target.prefixedProperties(""), evaluationContext));
        return builder.build();
    }

    private Object evaluate(final Object o, final Map<String, Object> contextVariables, boolean silentResolve) {
        StandardEvaluationContext evaluationContext = buildEvaluationContext(contextVariables);
        return evaluateObject(o, evaluationContext, silentResolve);
    }

    private StandardEvaluationContext buildEvaluationContext(Map<String, Object> contextVariables) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        evaluationContext.registerMethodFilter(Runtime.class, methods -> Collections.emptyList());
        evaluationContext.registerMethodFilter(ProcessBuilder.class, methods -> Collections.emptyList());

        if (spelFunctions != null) {
            spelFunctions.stream().forEach(f -> evaluationContext.registerFunction(f.getName(), f.getMethod()));
        }
        evaluationContext.setVariables(contextVariables);
        return evaluationContext;
    }

    private Object evaluateObject(final Object object, final EvaluationContext evaluationContext) {
        return evaluateObject(object, evaluationContext, false);
    }

    @SuppressWarnings("unchecked")
    private Object evaluateObject(final Object object, final EvaluationContext evaluationContext, boolean silentResolve) {
        Object inputEvaluatedValue;
        if (object instanceof String stringValue) {
            if (hasOnlyOneSpel(stringValue)) {
                inputEvaluatedValue = Strings.replaceExpression(stringValue, s -> evaluate(parser, evaluationContext, s), EVALUATION_STRING_PREFIX, EVALUATION_STRING_SUFFIX, EVALUATION_STRING_ESCAPE, silentResolve);
            } else {
                inputEvaluatedValue = Strings.replaceExpressions(stringValue, s -> evaluate(parser, evaluationContext, s), EVALUATION_STRING_PREFIX, EVALUATION_STRING_SUFFIX, EVALUATION_STRING_ESCAPE, silentResolve);
            }
        } else if (object instanceof Map map) {
            Map evaluatedMap = new LinkedHashMap();
            map.forEach(
                (key, value) -> {
                    Object keyValue = evaluateObject(key, evaluationContext, silentResolve);
                    Object valueValue = evaluateObject(value, evaluationContext, silentResolve);
                    evaluatedMap.put(keyValue, valueValue);
                    if (keyValue instanceof String stringKeyValue) {
                        evaluationContext.setVariable(stringKeyValue, valueValue);
                    }
                });
            inputEvaluatedValue = evaluatedMap;
        } else if (object instanceof List list) {
            List evaluatedList = new ArrayList<>();
            list.forEach(
                obj -> evaluatedList.add(evaluateObject(obj, evaluationContext, silentResolve))
            );
            inputEvaluatedValue = evaluatedList;
        } else if (object instanceof Set set) {
            Set evaluatedSet = new LinkedHashSet();
            set.forEach(
                obj -> evaluatedSet.add(evaluateObject(obj, evaluationContext, silentResolve))
            );
            inputEvaluatedValue = evaluatedSet;
        } else {
            inputEvaluatedValue = object;
        }

        return inputEvaluatedValue;
    }

    private Object evaluate(ExpressionParser parser, final EvaluationContext evaluationContext, String expressionAsString) {
        final Expression expression = parseExpression(parser, expressionAsString);

        try {
            Object result = expression.getValue(evaluationContext);
            if (result == null) {
                throw new EvaluationException(expressionAsString);
            }
            return result;
        } catch (org.springframework.expression.EvaluationException e) {
            Exception initialException = e;
            if (initialException.getCause() != null && initialException.getCause() instanceof InvocationTargetException) {
                initialException = (InvocationTargetException) e.getCause();
                if (initialException.getCause() != null) {
                    initialException = (Exception) initialException.getCause();
                }
            }
            throw new EvaluationException(expressionAsString, initialException);
        }
    }

    /**
     * If there is only one spel, it means it can be evaluated as a whole java Object.
     * ex: ${#webdriver} will retrieve the object Webdriver stored in the context
     * If there are multiple spel, it will be a String concatenation
     *
     * @param template
     * @return true if only one spel in template
     */
    private boolean hasOnlyOneSpel(String template) {
        return EVALUATION_OBJECT_PATTERN.matcher(template.trim()).matches();
    }

    private Expression parseExpression(ExpressionParser parser, String expressionAsString) {
        Expression expression;
        try {
            expression = parser.parseExpression(expressionAsString);
        } catch (ParseException e) {
            throw new EvaluationException(expressionAsString, e);
        }
        return expression;
    }
}
