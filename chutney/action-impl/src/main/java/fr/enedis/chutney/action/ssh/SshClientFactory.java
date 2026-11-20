/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import static java.util.Collections.singletonList;

import fr.enedis.chutney.action.spi.injectable.Target;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.UserAuthFactory;
import org.apache.sshd.client.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.client.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;

public class SshClientFactory {

    public static String DEFAULT_TIMEOUT = "5 s";

    public static ClientSession buildSSHClientSession(Target target, long timeout) throws IOException {
        List<Connection> hops = new ArrayList<>(Connection.tunnelFrom(target));
        hops.add(Connection.from(target));

        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        try {
            ClientSession session = null;
            for (Connection hop : hops) {
                SshdSocketAddress address = new SshdSocketAddress(hop.serverHost, hop.serverPort);
                if (session != null) {
                    address = session.startLocalPortForwarding(new SshdSocketAddress(SshdSocketAddress.LOCALHOST_IPV4, 0), address);
                }
                session = authenticatedSession(client, hop, address, timeout);
            }
            return session;
        } catch (IOException | RuntimeException e) {
            client.stop();
            throw e;
        }
    }

    /** Stops the client owning the session, which also closes every proxy session of the tunnel. */
    public static void stopClientOf(ClientSession session) {
        ((SshClient) session.getFactoryManager()).stop();
    }

    private static ClientSession authenticatedSession(SshClient client, Connection connection, SshdSocketAddress address, long timeout) throws IOException {
        client.setUserAuthFactories(getAuthFactory(connection));
        ClientSession session = client.connect(connection.username, address.getHostName(), address.getPort()).verify(timeout).getSession();
        configureSessionAuthMethod(session, connection);
        session.auth().verify(timeout);
        return session;
    }

    private static List<UserAuthFactory> getAuthFactory(Connection connection) {
        if (connection.usePrivateKey()) {
            return singletonList(UserAuthPublicKeyFactory.INSTANCE);
        }
        return singletonList(UserAuthPasswordFactory.INSTANCE);
    }

    private static ClientSession configureSessionAuthMethod(ClientSession session, Connection connection) {
        if (connection.usePrivateKey()) {
            FileKeyPairProvider provider = new FileKeyPairProvider(new File(connection.privateKey).toPath());
            provider.setPasswordFinder(FilePasswordProvider.of(connection.passphrase));
            session.setKeyIdentityProvider(provider);
        } else {
            session.addPasswordIdentity(connection.password);
        }
        return session;
    }

}
