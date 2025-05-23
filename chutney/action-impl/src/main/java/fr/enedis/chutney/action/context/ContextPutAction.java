/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.context;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ContextPutAction implements Action {

    private final Logger logger;
    private final Map<String, Object> entries;

    public ContextPutAction(Logger logger, @Input("entries") Map<String, Object> entries) {
        this.logger = logger;
        this.entries = ofNullable(entries).orElse(emptyMap());
    }

    @Override
    public ActionExecutionResult execute() {
        entries.forEach((key, value) -> logger.info("Adding to context " + key + " = " + prettyLog(value) + " " + logClassType(value)));
        return ActionExecutionResult.ok(entries);
    }

    private String prettyLog(Object value) {
        return switch (value) {
            case null -> "null";
            case String s -> s;
            case Object[] objects -> Arrays.toString(objects);
            case List<?> list -> Arrays.toString(list.toArray());
            case Map<?, ?> map -> Arrays.toString(map.entrySet().toArray());
            default -> value.toString();
        };
    }

    private String logClassType(Object value) {
        return value != null ? "(" + value.getClass().getSimpleName() + ")" : "";
    }
}
