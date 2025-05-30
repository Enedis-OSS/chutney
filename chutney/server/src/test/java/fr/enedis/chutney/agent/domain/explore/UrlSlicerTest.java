/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.agent.domain.explore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UrlSlicerTest {

    @ParameterizedTest
    @MethodSource("acceptedURLs")
    public void urlWrapper_build_with_valid_url(String url) {
        new UrlSlicer(url);
    }

    @Test()
    public void urlWrapper_build_with_invalid_url() {
        assertThatThrownBy(() -> new UrlSlicer("invalid url:12"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("acceptedURLs")
    public void urlWrapper_parse_host(String url) {
        String host = new UrlSlicer(url).host;
        assertThat(host).as("host").isEqualTo("somehost");
    }

    @ParameterizedTest
    @MethodSource("acceptedURLs")
    public void urlWrapper_parse_port(String url) {
        UrlSlicer slicedUrl = new UrlSlicer(url);
        assertThat(slicedUrl.host).as("host").isEqualTo("somehost");
        assertThat(slicedUrl.port).as("port").isEqualTo(12);
    }

    @ParameterizedTest
    @MethodSource("acceptedURLsWithoutPorts")
    public void should_use_default_port_protocol(String url, Integer defaultPort) {
        UrlSlicer slicedUrl = new UrlSlicer(url);
        assertThat(slicedUrl.host).as("host").isEqualTo("somehost");
        assertThat(slicedUrl.port).as("port").isEqualTo(defaultPort);
    }

    @Test
    public void should_throw_when_target_port_is_null() {
        Throwable thrown = catchThrowable(() -> {
            new UrlSlicer("fake://host-without-port/");
        });

        assertThat(thrown)
            .isInstanceOf(UndefinedPortException.class)
            .hasMessageContaining("Port is not defined on [fake://host-without-port/]. Cannot default port for [fake] protocol.");
    }

    private static String[] acceptedURLs() {
        return new String[]{
            "proto://somehost:12/",
            "proto://somehost:12/path",
            "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(host=somehost)(PORT=12))"
        };
    }

    private static Object[] acceptedURLsWithoutPorts() {
        return new Object[]{
            new Object[]{"http://somehost/", 80},
            new Object[]{"https://somehost", 443},
            new Object[]{"ssh://somehost", 22},
            new Object[]{"amqp://somehost", 5672},
            new Object[]{"amqps://somehost", 5671},
            new Object[]{"ftp://somehost", 20},
        };
    }
}
