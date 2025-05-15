/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium;

import static fr.enedis.chutney.action.selenium.parameter.SeleniumActionActionParameter.WEBDRIVER;

import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class SeleniumScreenShotAction extends SeleniumAction {

    public SeleniumScreenShotAction(Logger logger,
                                  @Input(WEBDRIVER) WebDriver webDriver) {
        super(logger, webDriver);
    }

    @Override
    public ActionExecutionResult executeSeleniumAction() {
        String screenShot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BASE64);
        logger.reportOnly().info("data:image/png;base64," + screenShot);

        return ActionExecutionResult.ok();
    }
}
