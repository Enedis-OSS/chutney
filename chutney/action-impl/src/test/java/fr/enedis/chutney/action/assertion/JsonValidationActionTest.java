/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.ActionExecutionResult.Status;
import fr.enedis.chutney.action.spi.injectable.Logger;
import org.junit.jupiter.api.Test;

public class JsonValidationActionTest {

    JsonValidationAction task;

    private final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-04/schema#",
            "title": "Product",
            "description": "A product from the catalog",
            "type": "object",
            "properties": {
                "id": {
                    "description": "The unique identifier for a product",
                    "type": "integer"
                },
                "name": {
                    "description": "Name of the product",
                    "type": "string"
                },
                "price": {
                    "type": "number",
                    "minimum": 0,
                    "exclusiveMinimum": true
                }
            },
            "required": ["id", "name", "price"]
        }\
        """;

    @Test
    public void should_validate_simple_json() {
        Logger logger = new TestLogger();
        String json = """
            {
                "id": 1,
                "name": "Lampshade",
                "price": 12
            }\
            """;


        task = new JsonValidationAction(logger, json, SCHEMA);

        //When
        ActionExecutionResult result = task.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);
    }


    @Test
    public void should_not_validate_simple_json() {
        Logger logger = new TestLogger();
        String json = """
            {
                "id": 1,
                "name": "Lampshade",
                "price": 0
            }\
            """;

        task = new JsonValidationAction(logger, json, SCHEMA);

        //When
        ActionExecutionResult result = task.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Failure);
    }


}
