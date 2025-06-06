/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh.sshj;


import fr.enedis.chutney.action.spi.time.Duration;
import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.util.Optional;

public class Command {

    private static final Duration DEFAULT_DURATION = Duration.parse("5000 ms");

    public final String command;
    public final Duration timeout;

    public Command(String command) {
        this.command = command;
        this.timeout = DEFAULT_DURATION;
    }

    Command(String command, String timeout) {
        this.command = command;
        this.timeout = Optional.ofNullable(timeout).map(Duration::parse).orElse(DEFAULT_DURATION);
    }

    CommandResult executeWith(SshClient sshClient) throws IOException {
        return sshClient.execute(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("command",command)
            .add("timeout",timeout)
            .toString();
    }
}
