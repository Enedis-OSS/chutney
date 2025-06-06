/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http.domain;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

public class HttpAction {

    public static ActionExecutionResult httpCall(Logger logger, Supplier<ResponseEntity<String>> caller) {
        try {
            ResponseEntity<String> response = caller.get();
            logger.info("HTTP call status :" + response.getStatusCode().value());
            return ActionExecutionResult.ok(toOutputs(response));
        }
        catch (ResourceAccessException e) {
            logger.error("HTTP call failed during execution: " + e.getMessage());
            return ActionExecutionResult.ko();
        }
    }

    private static Map<String, Object> toOutputs(ResponseEntity<String> response) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("status", response.getStatusCode().value());
        outputs.put("body", response.getBody());
        outputs.put("headers", response.getHeaders());
        return outputs;
    }
}
