/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium;

import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.SELECTOR;
import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.VALUE;
import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.WEBDRIVER;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Set;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class SeleniumGetAction extends SeleniumAction {

    private static final String SELENIUM_OUTPUTS_KEY = "outputGet";

    private final String selector;
    private final String value;

    public SeleniumGetAction(Logger logger,
                           @Input(WEBDRIVER) WebDriver webDriver,
                           @Input(SELECTOR) String selector,
                           @Input(VALUE) String value) {
        super(logger, webDriver);
        this.selector = selector;
        this.value = value;
    }

    @Override
    public ActionExecutionResult executeSeleniumAction() {
        if (selector != null) {
            Set<String> afterAllCurrentWindowHandles = getNewWindowHandle(webDriver);

            webDriver.switchTo().window((String) afterAllCurrentWindowHandles.toArray()[0]);
            logger.info("Switch to new window");
        }
        logger.info("Get url : " + value);
        webDriver.get(value);

        return ActionExecutionResult.ok(SELENIUM_OUTPUTS_KEY, webDriver.getWindowHandle());
    }

    private Set<String> getNewWindowHandle(WebDriver webDriver) {
        Set<String> allCurrentWindowHandles = webDriver.getWindowHandles();
        ((JavascriptExecutor) webDriver).executeScript("window.open();");
        Set<String> afterAllCurrentWindowHandles = webDriver.getWindowHandles();
        afterAllCurrentWindowHandles.removeAll(allCurrentWindowHandles);
        return afterAllCurrentWindowHandles;
    }
}
