/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.amqp;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestFinallyActionRegistry;
import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.FinallyAction;
import org.apache.qpid.server.SystemLauncher;
import org.junit.jupiter.api.Test;

class QpidServerStartActionTest {

    @Test
    void should_start_with_default_configuration() {
        ActionExecutionResult executionResult = null;
        try {
            TestLogger logger = new TestLogger();
            TestFinallyActionRegistry finallyActionRegistry = new TestFinallyActionRegistry();
            QpidServerStartAction sut = new QpidServerStartAction(logger, finallyActionRegistry, null);

            executionResult = sut.execute();

            assertThat(executionResult.status).isEqualTo(ActionExecutionResult.Status.Success);
            assertThat(executionResult.outputs)
                .hasSize(1)
                .extractingByKey("qpidLauncher").isInstanceOf(SystemLauncher.class);
            assertThat(logger.info).hasSize(2);
            assertThat(finallyActionRegistry.finallyActions)
                .hasSize(1)
                .hasOnlyElementsOfType(FinallyAction.class);

            assertThat(finallyActionRegistry.finallyActions.getFirst().type())
                .isEqualTo("qpid-server-stop");
        } finally {
            if (executionResult != null) {
                SystemLauncher qpidServer = (SystemLauncher) executionResult.outputs.get("qpidLauncher");
                qpidServer.shutdown();
            }
        }
    }

}
