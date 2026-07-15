/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium.jackson;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class WebDriverSerializer extends StdSerializer<WebDriver> {

    protected WebDriverSerializer() {
        super(WebDriver.class);
    }

    @Override
    public void serialize(WebDriver value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("driver", value.toString());
        if (value instanceof RemoteWebDriver driver) {
            gen.writeStringProperty("capabilities", driver.getCapabilities().asMap().toString());
        }
        gen.writeEndObject();
    }
}
