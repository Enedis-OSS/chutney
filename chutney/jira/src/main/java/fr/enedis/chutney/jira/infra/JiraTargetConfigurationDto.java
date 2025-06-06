/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.infra;

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

    public JiraTargetConfigurationDto(String url, String username, String password, String urlProxy, String userProxy, String passwordProxy) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.urlProxy = urlProxy;
        this.userProxy = userProxy;
        this.passwordProxy = passwordProxy;
    }
}
