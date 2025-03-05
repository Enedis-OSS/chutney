/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.selenium.driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.chutneytesting.action.TestFinallyActionRegistry;
import com.chutneytesting.action.TestLogger;
import com.chutneytesting.action.spi.Action;
import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.FinallyAction;
import com.chutneytesting.action.spi.injectable.FinallyActionRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public class SeleniumDriverInitActionTest {

    public static Stream<Arguments> parametersForShould_create_quit_finally_action_and_output_webdriver_when_executed() {
        TestLogger logger = new TestLogger();
        TestFinallyActionRegistry finallyActionRegistry = spy(new TestFinallyActionRegistry());

        SeleniumFirefoxDriverInitAction localFirefoxAction = spy(new SeleniumFirefoxDriverInitAction(finallyActionRegistry, logger, "", false, "driverPath", "browserPath", null, null));
        SeleniumChromeDriverInitAction localChromeAction = spy(new SeleniumChromeDriverInitAction(finallyActionRegistry, logger, "", false, "driverPath", "browserPath", null));
        SeleniumEdgeDriverInitAction localEdgeAction = spy(new SeleniumEdgeDriverInitAction(finallyActionRegistry, logger, "", false, "driverPath", "browserPath", null));

        WebDriver firefoxDriver = mock(FirefoxDriver.class);
        WebDriver chromeDriver = mock(ChromeDriver.class);
        WebDriver edgeDriver = mock(EdgeDriver.class);

        doReturn(firefoxDriver)
            .when(localFirefoxAction).localWebDriver(any());
        doReturn(chromeDriver)
            .when(localChromeAction).localWebDriver(any());
        doReturn(edgeDriver).when(localEdgeAction).localWebDriver(any());


        SeleniumFirefoxDriverInitAction remoteFirefoxAction = spy(new SeleniumFirefoxDriverInitAction(finallyActionRegistry, logger, "http://hub:99", false, "", "", null, null));
        SeleniumChromeDriverInitAction remoteChromeAction = spy(new SeleniumChromeDriverInitAction(finallyActionRegistry, logger, "http://hub:99", false, "", "", null));
        SeleniumEdgeDriverInitAction remoteEdgeAction = spy(new SeleniumEdgeDriverInitAction(finallyActionRegistry, logger, "http://hub:99", false, "", "", null));

        RemoteWebDriver firefoxRemoteWebDriver = mock(RemoteWebDriver.class);
        RemoteWebDriver chromeRemoteWebDriver = mock(RemoteWebDriver.class);
        RemoteWebDriver edgeRemoteWebDriver = mock(RemoteWebDriver.class);
        doReturn(firefoxRemoteWebDriver)
            .when(remoteFirefoxAction).createWebDriver(any());
        doReturn(chromeRemoteWebDriver)
            .when(remoteChromeAction).createWebDriver(any());
        doReturn(edgeRemoteWebDriver)
            .when(remoteEdgeAction).createWebDriver(any());

        String firefoxJsonConfiguration ="{\"acceptInsecureCerts\":true,\"browserName\":\"firefox\",\"moz:debuggerAddress\":true,\"moz:firefoxOptions\":{\"args\":[\"-headless\"],\"binary\":\"browserPath\",\"log\":{\"level\":\"debug\"}}}";
        SeleniumGenericDriverInitAction firefoxGenericSeleniumAction = spy(new SeleniumGenericDriverInitAction(finallyActionRegistry, logger, "http://hub:99", firefoxJsonConfiguration));

        RemoteWebDriver genericFirefoxRemoteWebDriver = mock(RemoteWebDriver.class);
        doReturn(genericFirefoxRemoteWebDriver)
            .when(firefoxGenericSeleniumAction).createWebDriver(any());

        return Stream.of(
            of(localFirefoxAction, firefoxDriver, finallyActionRegistry),
            of(localChromeAction, chromeDriver, finallyActionRegistry),
            of(localEdgeAction, edgeDriver, finallyActionRegistry),
            of(remoteFirefoxAction, firefoxRemoteWebDriver, finallyActionRegistry),
            of(remoteChromeAction, chromeRemoteWebDriver, finallyActionRegistry),
            of(remoteEdgeAction, edgeRemoteWebDriver, finallyActionRegistry),
            of(firefoxGenericSeleniumAction, genericFirefoxRemoteWebDriver, finallyActionRegistry)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForShould_create_quit_finally_action_and_output_webdriver_when_executed")
    public void should_create_quit_finally_action_and_output_webdriver_when_executed(AbstractSeleniumDriverInitAction action, WebDriver driverToAssert, TestFinallyActionRegistry finallyActionRegistry) {
        reset(finallyActionRegistry);

        // When
        List<String> strings = action.validateInputs();
        assertThat(strings).isEmpty();
        ActionExecutionResult result = action.execute();

        // Then
        verify(finallyActionRegistry, times(1)).registerFinallyAction(any());
        FinallyAction quitAction = finallyActionRegistry.finallyActions.getFirst();
        assertThat(quitAction.type()).isEqualTo("selenium-quit");
        assertThat(quitAction.inputs()).containsOnlyKeys("web-driver");

        assertThat(result.status).isEqualTo(ActionExecutionResult.Status.Success);
        assertThat(result.outputs).containsOnlyKeys("webDriver");
        assertThat(result.outputs).containsValues(driverToAssert);
        assertThat(result.outputs.get("webDriver")).isInstanceOf(driverToAssert.getClass());
    }

    @ParameterizedTest
    @MethodSource("parametersForshould_retun_error_when_wrong_input")
    public void should_retun_error_when_wrong_input(Action action) {
        assertThat(action.validateInputs()).hasSize(1);
    }

    public static Stream<Arguments> parametersForshould_retun_error_when_wrong_input() {
        Action everyInputEmpty = new SeleniumFirefoxDriverInitAction(mock(FinallyActionRegistry.class), null, "", false, "", "", null, null);
        Action chromeDriverPathOKBrowserPathEmpty = new SeleniumChromeDriverInitAction(mock(FinallyActionRegistry.class), null, "", false, "driverPath", "", null);
        Action chromeDriverPathEmptyBrowserPathOK = new SeleniumChromeDriverInitAction(mock(FinallyActionRegistry.class), null, "", false, "", "browserPath", null);
        Action edgeDriverPathOKBrowserPathEmpty = new SeleniumEdgeDriverInitAction(mock(FinallyActionRegistry.class), null, "", false, "driverPath", "", null);
        Action edgeDriverPathEmptyBrowserPathOK = new SeleniumEdgeDriverInitAction(mock(FinallyActionRegistry.class), null, "", false, "", "browserPath", null);

        Action everyInputNull = new SeleniumFirefoxDriverInitAction(mock(FinallyActionRegistry.class), null, null, false, null, null, null, null);
        Action chromeDriverPathOKBrowserPathNull = new SeleniumChromeDriverInitAction(mock(FinallyActionRegistry.class), null, null, false, "driverPath", null, null);
        Action chromeDriverPathNullBrowserPathOK = new SeleniumChromeDriverInitAction(mock(FinallyActionRegistry.class), null, null, false, null, "", null);
        Action edgeDriverPathOKBrowserPathNull = new SeleniumEdgeDriverInitAction(mock(FinallyActionRegistry.class), null, null, false, "driverPath", null, null);
        Action edgeDriverPathNullBrowserPathOK = new SeleniumEdgeDriverInitAction(mock(FinallyActionRegistry.class), null, null, false, null, "", null);

        return Stream.of(
            of(everyInputEmpty),
            of(chromeDriverPathOKBrowserPathEmpty),
            of(chromeDriverPathEmptyBrowserPathOK),
            of(edgeDriverPathOKBrowserPathEmpty),
            of(edgeDriverPathEmptyBrowserPathOK),
            of(everyInputNull),
            of(chromeDriverPathOKBrowserPathNull),
            of(chromeDriverPathNullBrowserPathOK),
            of(edgeDriverPathOKBrowserPathNull),
            of(edgeDriverPathNullBrowserPathOK)
        );
    }

    @Test
    void name() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("-headless");
        firefoxOptions.setLogLevel(FirefoxDriverLogLevel.DEBUG);
        firefoxOptions.setBinary("browserPath");
        String jsonFirefoxOptions = mapper.writeValueAsString(firefoxOptions.toJson());

        Map<String, Object> readFromJson = mapper.readValue(jsonFirefoxOptions, new TypeReference<>() {
        });
        Capabilities unknownCaps = new MutableCapabilities(readFromJson);

        assertThat(firefoxOptions.asMap()).containsExactlyEntriesOf(unknownCaps.asMap());
    }
}
