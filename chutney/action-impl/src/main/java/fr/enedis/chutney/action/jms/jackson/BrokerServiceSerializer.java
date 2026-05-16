/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms.jackson;

import org.apache.activemq.broker.BrokerService;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class BrokerServiceSerializer extends StdSerializer<BrokerService> {

    protected BrokerServiceSerializer() {
        super(BrokerService.class);
    }

    @Override
    public void serialize(BrokerService value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("ActiveMQ Broker Service", value.toString());
        gen.writeEndObject();
    }
}
