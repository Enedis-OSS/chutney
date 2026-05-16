/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.infra;

public class JiraTargetConfigurationDto {
    public String url;
    public String username;
    public String password;
    public String urlProxy;
    public String userProxy;
    public String passwordProxy;

    @com.fasterxml.jackson.annotation.JsonCreator
    public JiraTargetConfigurationDto() {
        this("", "", "", "", "", "");
    }

    public JiraTargetConfigurationDto(String url, String username, String password, String urlProxy, String userProxy, String passwordProxy) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.urlProxy = urlProxy;
        this.userProxy = userProxy;
        this.passwordProxy = passwordProxy;
    }
}
