/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.kafka.jackson;

import org.springframework.kafka.test.EmbeddedKafkaBroker;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class EmbeddedKafkaBrokerSerializer extends StdSerializer<EmbeddedKafkaBroker> {

    protected EmbeddedKafkaBrokerSerializer() {
        super(EmbeddedKafkaBroker.class);
    }

    @Override
    public void serialize(EmbeddedKafkaBroker value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("Embedded Kafka Broker", brokersAsString(value));
        gen.writeEndObject();
    }

    private static String brokersAsString(EmbeddedKafkaBroker value) {
        try {
            return value.getBrokersAsString();
        } catch (RuntimeException e) {
            return "shut down";
        }
    }
}
