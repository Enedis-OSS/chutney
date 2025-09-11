/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestTarget;
import fr.enedis.chutney.action.spi.injectable.Target;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class ConnectionTest {

    @Test
    void build_connection_from_target() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withUrl("ssh://ssh-host:1234")
            .withProperty("user", "sshUser")
            .withProperty("password", "sshPassword")
            .withProperty("privateKey", "/privateKeyPath")
            .withProperty("privateKeyPassword", "privateKeyPassphrase")
            .build();
        Connection connection = Connection.from(target);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(connection.serverHost).isEqualTo(target.host());
            softly.assertThat(connection.serverPort).isEqualTo(target.port());
            softly.assertThat(connection.username).isEqualTo(target.user().get());
            softly.assertThat(connection.password).isEqualTo(target.userPassword().get());
            softly.assertThat(connection.usePrivateKey()).isTrue();
            softly.assertThat(connection.privateKey).isEqualTo(target.privateKey().get());
            softly.assertThat(connection.passphrase).isEqualTo(target.privateKeyPassword().get());
        });
    }

    @Test
    void build_tunnel_connection_from_target_proxy() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withUrl("ssh://ssh-host")
            .withProperty("proxy", "ssh://proxy-host")
            .withProperty("proxyUser", "sshUser")
            .withProperty("proxyPassword", "sshPassword")
            .withProperty("proxyPrivateKey", "/privateKeyPath")
            .withProperty("proxyPassphrase", "privateKeyPassphrase")
            .build();
        List<Connection> tunnelConnections = Connection.tunnelFrom(target);

        assertThat(tunnelConnections).hasSize(1);
        Connection connection = tunnelConnections.getFirst();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(connection.serverHost).isEqualTo("proxy-host");
            softly.assertThat(connection.serverPort).isEqualTo(22);
            softly.assertThat(connection.username).isEqualTo("sshUser");
            softly.assertThat(connection.password).isEqualTo("sshPassword");
            softly.assertThat(connection.usePrivateKey()).isTrue();
            softly.assertThat(connection.privateKey).isEqualTo("/privateKeyPath");
            softly.assertThat(connection.passphrase).isEqualTo("privateKeyPassphrase");
        });
    }

    @Test
    void build_tunnel_connection_from_target_proxies() {
        Target target = TestTarget.TestTargetBuilder.builder()
            .withUrl("ssh://ssh-host")
            .withProperty("proxy_1", "ssh://proxy-host-one")
            .withProperty("proxyUser_1", "sshUserOne")
            .withProperty("proxyPassword_1", "sshPasswordOne")
            .withProperty("proxyPrivateKey_1", "/privateKeyPathOne")
            .withProperty("proxyPassphrase_1", "privateKeyPassphraseOne")
            .withProperty("proxy_2", "ssh://proxy-host-two:1234")
            .withProperty("proxyUser_2", "sshUserTwo")
            .withProperty("proxyPassword_2", "sshPasswordTwo")
            .withProperty("proxy_4", "ssh://proxy-host-three:2222")
            .withProperty("proxyPrivateKey_4", "/privateKeyPathThree")
            .withProperty("proxyPassphrase_4", "privateKeyPassphraseThree")
            .build();
        List<Connection> tunnelConnections = Connection.tunnelFrom(target);

        assertThat(tunnelConnections).hasSize(3);
        final Connection firstConnection = tunnelConnections.getFirst();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(firstConnection.serverHost).isEqualTo("proxy-host-one");
            softly.assertThat(firstConnection.serverPort).isEqualTo(22);
            softly.assertThat(firstConnection.username).isEqualTo("sshUserOne");
            softly.assertThat(firstConnection.password).isEqualTo("sshPasswordOne");
            softly.assertThat(firstConnection.usePrivateKey()).isTrue();
            softly.assertThat(firstConnection.privateKey).isEqualTo("/privateKeyPathOne");
            softly.assertThat(firstConnection.passphrase).isEqualTo("privateKeyPassphraseOne");
        });
        final Connection secondConnection = tunnelConnections.get(1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(secondConnection.serverHost).isEqualTo("proxy-host-two");
            softly.assertThat(secondConnection.serverPort).isEqualTo(1234);
            softly.assertThat(secondConnection.username).isEqualTo("sshUserTwo");
            softly.assertThat(secondConnection.password).isEqualTo("sshPasswordTwo");
            softly.assertThat(secondConnection.usePrivateKey()).isFalse();
        });
        final Connection thirdConnection = tunnelConnections.get(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(thirdConnection.serverHost).isEqualTo("proxy-host-three");
            softly.assertThat(thirdConnection.serverPort).isEqualTo(2222);
            softly.assertThat(thirdConnection.usePrivateKey()).isTrue();
            softly.assertThat(thirdConnection.privateKey).isEqualTo("/privateKeyPathThree");
            softly.assertThat(thirdConnection.passphrase).isEqualTo("privateKeyPassphraseThree");
        });
    }
}
