/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http.jackson;

import com.github.tomakehurst.wiremock.WireMockServer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class WireMockServerSerializer extends StdSerializer<WireMockServer> {

    protected WireMockServerSerializer() {
        super(WireMockServer.class);
    }

    @Override
    public void serialize(WireMockServer value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("https-server-instance", value.toString());
        // TODO - Eventually add some information on wiremockserver
        gen.writeEndObject();
    }
}
