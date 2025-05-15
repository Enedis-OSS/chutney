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
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

public class SeleniumEdgeDriverInitAction extends AbstractSeleniumDriverInitAction {

    private final List<String> edgeOptions;

    public SeleniumEdgeDriverInitAction(FinallyActionRegistry finallyActionRegistry,
                                        Logger logger,
                                        @Input("hub") String hubUrl,
                                        @Input("headless") Boolean headless,
                                        @Input("driverPath") String driverPath,
                                        @Input("browserPath") String browserPath,
                                        @Input("edgeOptions") List<String> edgeOptions) {
        super(finallyActionRegistry, logger, hubUrl, headless, driverPath, browserPath);
        this.edgeOptions = Optional.ofNullable(edgeOptions).orElse(List.of());
    }

    @Override
    protected MutableCapabilities buildOptions() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("start-maximized");
        if (headless) {
            options.addArguments("--headless");
        }
        edgeOptions.forEach(options::addArguments);
        options.setCapability(EdgeOptions.CAPABILITY, options);
        return options;
    }

    @Override
    protected WebDriver localWebDriver(Capabilities capabilities) {
        System.setProperty("webdriver.edge.driver", driverPath);
        EdgeOptions edgeOptions = new EdgeOptions().merge(capabilities);
        edgeOptions.setBinary(browserPath);
        return new EdgeDriver(edgeOptions);
    }

    @Override
    protected Class<?> getChildClass() {
        return SeleniumEdgeDriverInitAction.class;
    }
}
