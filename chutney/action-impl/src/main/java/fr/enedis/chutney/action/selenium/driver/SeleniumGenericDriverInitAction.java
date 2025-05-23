/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium.driver;

import fr.enedis.chutney.action.spi.injectable.FinallyActionRegistry;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

public class SeleniumGenericDriverInitAction extends AbstractSeleniumDriverInitAction {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String seleniumConfigurationAsJson;
    public SeleniumGenericDriverInitAction(FinallyActionRegistry finallyActionRegistry,
                                              Logger logger,
                                              @Input("hub") String hubUrl,
                                              @Input("jsonConfiguration") String seleniumConfigurationAsJson) {
        super(finallyActionRegistry, logger, hubUrl, null, null, null);
        this.seleniumConfigurationAsJson = seleniumConfigurationAsJson;
    }

    @Override
    protected MutableCapabilities buildOptions() {
        Map<String, Object> readFromJson;
        try {
            readFromJson = mapper.readValue(seleniumConfigurationAsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parsing error. Cannot transform json configuration to Selenium capabilities");
        }
        return new MutableCapabilities(readFromJson);
    }

    @Override
    protected WebDriver localWebDriver(Capabilities capabilities) {
        throw new IllegalStateException("Cannot create generic local web driver");
    }

    @Override
    protected Class<?> getChildClass() {
        return SeleniumGenericDriverInitAction.class;
    }
}
