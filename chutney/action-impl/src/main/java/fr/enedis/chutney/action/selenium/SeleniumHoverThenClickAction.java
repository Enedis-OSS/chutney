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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class SeleniumHoverThenClickAction extends SeleniumAction implements SeleniumFindBehavior {

    private final String selector;
    private final String by;
    private final Integer wait;

    public SeleniumHoverThenClickAction(Logger logger,
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
        Optional<WebElement> menuHoverWebElement = findElement(logger, webDriver, selector, by, wait);
        Actions actions = new Actions(webDriver);

        if (menuHoverWebElement.isPresent()) {
            actions.moveToElement(menuHoverWebElement.get()).build().perform();
            SeleniumClickAction seleniumClickAction = new SeleniumClickAction(logger, webDriver, selector, by, wait);
            return seleniumClickAction.execute();
        }
        takeScreenShot();
        return ActionExecutionResult.ko();
    }
}
