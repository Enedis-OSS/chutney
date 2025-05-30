/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.infrastructure.delegation;

import fr.enedis.chutney.engine.domain.delegation.ConnectionChecker;
import fr.enedis.chutney.engine.domain.delegation.NamedHostAndPort;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TcpConnectionChecker implements ConnectionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TcpConnectionChecker.class);

    private static final int TIMEOUT = 1000;

    @Override
    public boolean canConnectTo(NamedHostAndPort namedHostAndPort) {
        boolean reached = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(namedHostAndPort.host(), namedHostAndPort.port()), TIMEOUT);
            reached = true;
        } catch (IOException e) {
            LOGGER.warn("Unable to connect to {} ({}: {})", namedHostAndPort, e.getClass().getSimpleName(), e.getMessage());
        }
        return reached;
    }
}
