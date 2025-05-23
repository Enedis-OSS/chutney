/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public abstract class SeleniumAction implements Action {

    protected final Logger logger;
    protected final WebDriver webDriver;

    protected SeleniumAction(Logger logger, WebDriver webDriver) {
        this.logger = logger;
        this.webDriver = webDriver;
    }

    protected abstract ActionExecutionResult executeSeleniumAction();

    @Override
    public final ActionExecutionResult execute() {
        try {
            return this.executeSeleniumAction();
        } catch (Exception e) {
            logger.error(e.toString());
            takeScreenShot();
            return ActionExecutionResult.ko();
        }
    }

    protected void takeScreenShot() {
        try {
            String screenShot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BASE64);
            logger.error("data:image/png;base64," + screenShot);
        } catch (ClassCastException e) {
            logger.error("WebDriver could not take screenshots.");
        } catch (Exception e) {
            logger.error("Error taking screenshot : " + e.getMessage());
        }
    }
}
