/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh;

import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.ssh.sshd.SshServerMock;
import java.io.IOException;

public class SshServerStopAction implements Action {

    private final Logger logger;
    private final SshServerMock sshServer;

    public SshServerStopAction(Logger logger, @Input("ssh-server") SshServerMock sshServer) {
        this.logger = logger;
        this.sshServer = sshServer;
    }


    @Override
    public ActionExecutionResult execute() {
        try {
            sshServer.stop();
            logger.info("SshServer instance " + sshServer + " closed");
            return ActionExecutionResult.ok();
        } catch (IOException ioe) {
            logger.error(ioe);
            return ActionExecutionResult.ko();
        }
    }
}
