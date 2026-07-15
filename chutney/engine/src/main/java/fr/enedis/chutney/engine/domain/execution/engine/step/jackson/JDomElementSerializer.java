/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.execution.engine.step.jackson;

import java.io.Serial;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class JDomElementSerializer extends StdSerializer<Element> {

    @Serial
    private static final long serialVersionUID = 1L;

    public JDomElementSerializer() {
        this(null);
    }

    protected JDomElementSerializer(Class<Element> t) {
        super(t);
    }

    @Override
    public void serialize(Element element, JsonGenerator jsonGenerator, SerializationContext serializerProvider) throws JacksonException {
        String xmlString = new XMLOutputter(Format.getCompactFormat()).outputString(element);
        jsonGenerator.writeString(xmlString);
    }
}
