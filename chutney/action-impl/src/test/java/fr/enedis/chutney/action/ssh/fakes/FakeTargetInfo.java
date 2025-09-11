/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh.fakes;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sshd.server.SshServer;

public class FakeTargetInfo {

    private static final String RSA_PRIVATE_KEY = FakeTargetInfo.class.getResource("/security/client_rsa.key").getPath();
    private static final String ECDSA_PRIVATE_KEY = FakeTargetInfo.class.getResource("/security/client_ecdsa.key").getPath();

    public static Target buildTargetWithPassword(SshServer sshServer) {
        return buildTarget(sshServer, FakeServerSsh.PASSWORD, null, null, emptyList(), null, null);
    }

    public static Target buildTargetWithPrivateKeyWithoutPassphrase(SshServer sshServer) {
        return buildTarget(sshServer, null, RSA_PRIVATE_KEY, null, emptyList(), null, null);
    }

    public static Target buildTargetWithPrivateKeyWithPassphrase(SshServer sshServer) {
        return buildTarget(sshServer, null, ECDSA_PRIVATE_KEY, "password", emptyList(), null, null);
    }

    public static Target buildTargetWithPassword(SshServer sshServer, String proxy, String proxyUser, String proxyPassword) {
        return buildTarget(sshServer, FakeServerSsh.PASSWORD, null, null, List.of(proxy), List.of(proxyUser), List.of(proxyPassword));
    }

    public static Target buildTargetWithPassword(SshServer sshServer, List<String> proxy, List<String> proxyUser, List<String> proxyPassword) {
        return buildTarget(sshServer, FakeServerSsh.PASSWORD, null, null, proxy, proxyUser, proxyPassword);
    }

    private static Target buildTarget(
        SshServer sshServer,
        String userPassword,
        String privateKeyPath,
        String privateKeyPassphrase,
        List<String> proxies,
        List<String> proxyUsers,
        List<String> proxyPasswords
    ) {
        Map<String, String> properties = new HashMap<>();
        properties.put("user", FakeServerSsh.USERNAME);
        ofNullable(userPassword).ifPresent(cp -> properties.put("password", cp));
        ofNullable(privateKeyPath).ifPresent(pkp -> properties.put("privateKey", pkp));
        ofNullable(privateKeyPassphrase).ifPresent(pkp -> properties.put("privateKeyPassphrase", pkp));

        for (int i = 0; i < proxies.size(); i++) {
            int finalI = i;
            ofNullable(proxies.get(i)).ifPresent(pkp -> properties.put("proxy_" + finalI, pkp));
        }
        for (int i = 0; i < proxies.size(); i++) {
            int finalI = i;
            ofNullable(proxyUsers.get(i)).ifPresent(pkp -> properties.put("proxyUser_" + finalI, pkp));
        }
        for (int i = 0; i < proxies.size(); i++) {
            int finalI = i;
            ofNullable(proxyPasswords.get(i)).ifPresent(pkp -> properties.put("proxyPassword_" + finalI, pkp));
        }

        return new HardcodedTarget(sshServer, properties);
    }
}
