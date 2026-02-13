/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.selenium;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import fr.enedis.chutney.action.TestFinallyActionRegistry;
import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.selenium.driver.SeleniumChromeDriverInitAction;
import fr.enedis.chutney.action.selenium.driver.SeleniumEdgeDriverInitAction;
import fr.enedis.chutney.action.selenium.driver.SeleniumFirefoxDriverInitAction;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class SeleniumTest {

    private final Network network = Network.newNetwork();

    @BeforeAll
    static void beforeAll() {
        System.setProperty("api.version", "1.44");
    }

    @Test
    public void selenium_firefox_remote_driver_integration_test() {
        final BrowserWebDriverContainer firefoxWebDriverContainer = (BrowserWebDriverContainer) new BrowserWebDriverContainer()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network);

        try (firefoxWebDriverContainer) {
            firefoxWebDriverContainer.start();

            TestLogger logger = new TestLogger();
            TestFinallyActionRegistry finallyActionRegistry = spy(new TestFinallyActionRegistry());
            String url = firefoxWebDriverContainer.getSeleniumAddress().toString();

            SeleniumFirefoxDriverInitAction remoteFirefoxAction = new SeleniumFirefoxDriverInitAction(finallyActionRegistry, logger, url, true, null, null, null, null);
            ActionExecutionResult firefoxActionResult = remoteFirefoxAction.execute();
            assertThat(firefoxActionResult.status).isEqualTo(Success);

            SeleniumQuitAction quitAction = new SeleniumQuitAction(logger, (WebDriver) firefoxActionResult.outputs.get("webDriver"));
            quitAction.execute();
        }
    }

    @Test
    public void selenium_chrome_remote_driver_integration_test() {
        final BrowserWebDriverContainer chromeWebDriverContainer = (BrowserWebDriverContainer) new BrowserWebDriverContainer()
            .withCapabilities(new ChromeOptions())
            .withNetwork(network);
        try (chromeWebDriverContainer) {
            chromeWebDriverContainer.start();

            TestLogger logger = new TestLogger();
            TestFinallyActionRegistry finallyActionRegistry = spy(new TestFinallyActionRegistry());
            String url = chromeWebDriverContainer.getSeleniumAddress().toString();

            SeleniumChromeDriverInitAction remoteChromeAction = new SeleniumChromeDriverInitAction(finallyActionRegistry, logger, url, true, null, null, null);
            ActionExecutionResult chromeActionResult = remoteChromeAction.execute();
            assertThat(chromeActionResult.status).isEqualTo(Success);

            SeleniumQuitAction quitAction = new SeleniumQuitAction(logger, (WebDriver) chromeActionResult.outputs.get("webDriver"));
            quitAction.execute();
        }
    }

    @Test
    public void selenium_edge_remote_driver_integration_test() {
        final BrowserWebDriverContainer edgeWebDriverContainer = (BrowserWebDriverContainer) new BrowserWebDriverContainer()
            .withCapabilities(new EdgeOptions())
            .withNetwork(network);
        try (edgeWebDriverContainer) {
            edgeWebDriverContainer.start();

            TestLogger logger = new TestLogger();
            TestFinallyActionRegistry finallyActionRegistry = spy(new TestFinallyActionRegistry());
            String url = edgeWebDriverContainer.getSeleniumAddress().toString();

            SeleniumEdgeDriverInitAction remoteEdgeAction = new SeleniumEdgeDriverInitAction(finallyActionRegistry, logger, url, true, null, null, null);
            ActionExecutionResult edgeActionResult = remoteEdgeAction.execute();
            assertThat(edgeActionResult.status).isEqualTo(Success);

            SeleniumQuitAction quitAction = new SeleniumQuitAction(logger, (WebDriver) edgeActionResult.outputs.get("webDriver"));
            quitAction.execute();
        }
    }
}
