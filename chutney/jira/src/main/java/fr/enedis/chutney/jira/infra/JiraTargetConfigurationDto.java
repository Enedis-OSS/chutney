/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.infra;

import java.util.Objects;

public class JiraTargetConfigurationDto {
    public final String url;
    public final String username;
    public final String password;
    public final String urlProxy;
    public final String userProxy;
    public final String passwordProxy;

    public JiraTargetConfigurationDto() {
        this("", "", "", "", "", "");
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public JiraTargetConfigurationDto(
        @com.fasterxml.jackson.annotation.JsonProperty("url") String url,
        @com.fasterxml.jackson.annotation.JsonProperty("username") String username,
        @com.fasterxml.jackson.annotation.JsonProperty("password") String password,
        @com.fasterxml.jackson.annotation.JsonProperty("urlProxy") String urlProxy,
        @com.fasterxml.jackson.annotation.JsonProperty("userProxy") String userProxy,
        @com.fasterxml.jackson.annotation.JsonProperty("passwordProxy") String passwordProxy) {
        this.url = Objects.requireNonNullElse(url, "");
        this.username = Objects.requireNonNullElse(username, "");
        this.password = Objects.requireNonNullElse(password, "");
        this.urlProxy = Objects.requireNonNullElse(urlProxy, "");
        this.userProxy = Objects.requireNonNullElse(userProxy, "");
        this.passwordProxy = Objects.requireNonNullElse(passwordProxy, "");
    }
}
