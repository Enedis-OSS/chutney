/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.api;

import static fr.enedis.chutney.ServerConfigurationValues.SERVER_INSTANCE_NAME_VALUE;
import static fr.enedis.chutney.admin.api.InfoController.BASE_URL;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE_URL)
public class InfoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoController.class);
    public static final String BASE_URL = "/api/v1/info";
    private final Properties buildProperties;

    @Value(SERVER_INSTANCE_NAME_VALUE)
    private String applicationName;

    public InfoController() {
        buildProperties = loadBuildProperties();
    }

    private Properties loadBuildProperties() {
        Properties props = new Properties();
        try (InputStream input = InfoController.class.getClassLoader().getResourceAsStream("META-INF/build.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ioe) {
            LOGGER.warn("Cannot read build properties file", ioe);
        }
        return props;
    }

    @GetMapping(path = "/build/version")
    public String buildVersion() {
        return buildProperties.getProperty("build.version", "");
    }

    @GetMapping(path = "/appname")
    public String applicationName() {
        return applicationName;
    }
}
