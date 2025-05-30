/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra.ldap.ssl;

import java.util.Hashtable;
import org.springframework.ldap.core.support.SimpleDirContextAuthenticationStrategy;

public class ChutneyDirContextAuthenticationStrategy extends SimpleDirContextAuthenticationStrategy {

    @Override
    public void setupEnvironment(Hashtable<String, Object> env, String userDn, String password) {
        env.put("java.naming.ldap.factory.socket", "fr.enedis.chutney.security.infra.ldap.ssl.CustomSSLSocketFactory");
        super.setupEnvironment(env, userDn, password);
    }
}
