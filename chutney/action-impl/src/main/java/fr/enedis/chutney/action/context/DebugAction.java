/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.context;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DebugAction implements Action {

    private final Logger logger;
    private final Map<String, Object> inputs;
    private final List<String> filter;

    public DebugAction(Logger logger, Map<String, Object> inputs, @Input("filters") List<String> filter) {
        this.logger = logger;
        this.inputs = inputs;
        this.filter = Optional.ofNullable(filter).orElseGet(Collections::emptyList);
    }

    @Override
    public ActionExecutionResult execute() {
        inputs.entrySet().stream()
            .filter(entry -> filter.isEmpty() || filter.contains(entry.getKey()))
            .forEach(entry -> logger.info(entry.getKey() + " : [" + entry.getValue() + "]"));
        return ActionExecutionResult.ok();
    }
}
