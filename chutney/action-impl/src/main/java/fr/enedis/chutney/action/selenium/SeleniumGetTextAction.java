/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium;

import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.BY;
import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.SELECTOR;
import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.WAIT;
import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.WEBDRIVER;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.Optional;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SeleniumGetTextAction extends SeleniumAction implements SeleniumFindBehavior {

    private static final String SELENIUM_OUTPUTS_KEY = "outputGetText";

    private final String selector;
    private final String by;
    private final Integer wait;

    public SeleniumGetTextAction(Logger logger,
                               @Input(WEBDRIVER) WebDriver webDriver,
                               @Input(SELECTOR) String selector,
                               @Input(BY) String by,
                               @Input(WAIT) Integer wait) {
        super(logger, webDriver);
        this.selector = selector;
        this.by = by;
        this.wait = wait;
    }

    @Override
    public ActionExecutionResult executeSeleniumAction() {
        Optional<WebElement> webElement = findElement(logger, webDriver, selector, by, wait);

        if (webElement.isPresent()) {
            logger.info("Get text from element : " + webElement.get());
            ((JavascriptExecutor) webDriver).executeScript("arguments[0].style.background='yellow'", webElement.get());
            if (!webElement.get().getText().isEmpty()) {
                return ActionExecutionResult.ok(SELENIUM_OUTPUTS_KEY, webElement.get().getText());
            } else {
                return ActionExecutionResult.ok(SELENIUM_OUTPUTS_KEY, webElement.get().getAttribute("value"));
            }
        } else {
            takeScreenShot();
            logger.error("Cannot retrieve element to get text from.");
            return ActionExecutionResult.ko();
        }
    }
}
