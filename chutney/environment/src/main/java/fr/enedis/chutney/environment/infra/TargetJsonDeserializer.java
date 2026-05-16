/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.infra;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;

public class TargetJsonDeserializer extends ValueDeserializer<JsonTarget> {

    private static final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public JsonTarget deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonNode targetNode = deserializationContext.readTree(jsonParser);

        String name = null;
        if (targetNode.hasNonNull("name")) {
            name = targetNode.get("name").textValue();
        }

        String url = null;
        if (targetNode.hasNonNull("url")) {
            url = targetNode.get("url").textValue();
        }

        Map<String, String> properties = new HashMap<>();
        if (targetNode.hasNonNull("properties")) {
            properties = mapper.readValue(targetNode.get("properties").toString(), new TypeReference<>() {
            });
        }

        if (targetNode.hasNonNull("security")) {
            JsonNode security = targetNode.get("security");
            if (security.hasNonNull("trustStore")) {
                properties.put("trustStore", security.get("trustStore").textValue());
            }
            if (security.hasNonNull("trustStorePassword")) {
                properties.put("trustStorePassword", security.get("trustStorePassword").textValue());
            }
            if (security.hasNonNull("keyStore")) {
                properties.put("keyStore", security.get("keyStore").textValue());
            }
            if (security.hasNonNull("keyStorePassword")) {
                properties.put("keyStorePassword", security.get("keyStorePassword").textValue());
            }
            if (security.hasNonNull("keyPassword")) {
                properties.put("keyPassword", security.get("keyPassword").textValue());
            }
            if (security.hasNonNull("privateKey")) {
                properties.put("privateKey", security.get("privateKey").textValue());
            }
            if (security.hasNonNull("credential")) {
                JsonNode jsonCredential = security.get("credential");
                if (jsonCredential.hasNonNull("username")) {
                    properties.put("username", jsonCredential.get("username").textValue());
                    properties.put("password", ofNullable(jsonCredential.get("password")).map(JsonNode::textValue).orElse(""));
                }
            }
        }

        return new JsonTarget(name, url, properties);
    }
}
