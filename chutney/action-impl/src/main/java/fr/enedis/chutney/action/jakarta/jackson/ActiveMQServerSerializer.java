/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta.jackson;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ActiveMQServerSerializer extends StdSerializer<ActiveMQServer> {

    protected ActiveMQServerSerializer() {
        super(ActiveMQServer.class);
    }

    @Override
    public void serialize(ActiveMQServer value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("ActiveMQ Broker Service", value.toString());
        gen.writeEndObject();
    }
}

