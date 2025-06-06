/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh.sshd;

import java.io.IOException;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

public class ChutneyCommandFactory implements org.apache.sshd.server.command.CommandFactory {

    private final SshServerMock mock;

    public ChutneyCommandFactory(SshServerMock mock) {
        this.mock = mock;
    }

    @Override
    public Command createCommand(ChannelSession channel, String command) throws IOException {
        return new fr.enedis.chutney.action.ssh.sshd.Command(mock, command);
    }
}
