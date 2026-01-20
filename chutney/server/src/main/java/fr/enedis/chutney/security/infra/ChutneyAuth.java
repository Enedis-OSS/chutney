/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="chutney.auth")
public class ChutneyAuth {

    private boolean enableUserPassword;
    private boolean enableSso;

    public boolean isEnableUserPassword() {
        return enableUserPassword;
    }

    public boolean isEnableSso() {
        return enableSso;
    }

    public void setEnableUserPassword(boolean enableUserPassword) {
        this.enableUserPassword = enableUserPassword;
    }

    public void setEnableSso(boolean enableSso) {
        this.enableSso = enableSso;
    }

}
