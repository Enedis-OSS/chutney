/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium.jackson;

import org.openqa.selenium.WebDriver;
import tools.jackson.databind.module.SimpleModule;

public class SeleniumModule extends SimpleModule {

    private static final String NAME = "ChutneySeleniumModule";

    public SeleniumModule() {
        super(NAME);
        addSerializer(WebDriver.class, new WebDriverSerializer());
    }
}
