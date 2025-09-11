/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import static java.util.Collections.emptyList;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

import fr.enedis.chutney.action.spi.injectable.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Connection {

    private static final String EMPTY = "";

    public final String serverHost;
    public final int serverPort;
    public final String username;
    public final String password;
    public final String privateKey;
    public final String passphrase;

    private Connection(String serverHost, int serverPort, String username, String password, String privateKey, String passphrase) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    public static Connection from(Target target) {
        guardClause(target);

        final String host = target.host();
        final int port = extractPort(target);
        final String username = target.user().orElse(EMPTY);
        final String password = target.userPassword().orElse(EMPTY);
        final String privateKey = target.privateKey().orElse(EMPTY);
        final String passphrase = target.privateKeyPassword().orElse(EMPTY);

        return new Connection(host, port, username, password, privateKey, passphrase);
    }

    public static List<Connection> tunnelFrom(Target target) {
        long proxyIterations = target.prefixedProperties("proxy_").size();

        if (proxyIterations == 0) {
            return target.property("proxy")
                .map(proxy -> List.of(connectionFrom(target, proxy, "")))
                .orElse(emptyList());
        } else {
            List<Map.Entry<String, String>> proxies = target.prefixedProperties("proxy_").entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(key -> Integer.parseInt(key.split("_")[1]))))
                .toList();

            return proxies.stream().map(proxyEntry -> {
                int numIteration = Integer.parseInt(proxyEntry.getKey().split("_")[1]);
                return connectionFrom(target, proxyEntry.getValue(), "_" + numIteration);
            }).toList();
        }
    }

    private static Connection connectionFrom(Target target, String proxy, String suffix) {
        try {
            URI proxyUri = new URI(proxy);
            final String proxyHost = proxyUri.getHost();
            final int proxyPort = proxyUri.getPort() == -1 ? 22 : proxyUri.getPort();
            final String proxyUsername = target.property("proxyUser" + suffix).orElse(EMPTY);
            final String proxyPassword = target.property("proxyPassword" + suffix).orElse(EMPTY);
            final String proxyPrivateKey = target.property("proxyPrivateKey" + suffix).orElse(EMPTY);
            final String proxyPassphrase = target.property("proxyPassphrase" + suffix).orElse(EMPTY);

            return new Connection(proxyHost, proxyPort, proxyUsername, proxyPassword, proxyPrivateKey, proxyPassphrase);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean usePrivateKey() {
        return isNotBlank(privateKey);
    }

    private static void guardClause(Target target) {
        if (target.uri() == null) {
            throw new IllegalArgumentException("Target URL is undefined");
        }
        if (target.host() == null || target.host().isEmpty()) {
            throw new IllegalArgumentException("Target is badly defined");
        }
    }

    private static int extractPort(Target target) {
        int serverPort = target.port();
        return serverPort == -1 ? 22 : serverPort;
    }
}
