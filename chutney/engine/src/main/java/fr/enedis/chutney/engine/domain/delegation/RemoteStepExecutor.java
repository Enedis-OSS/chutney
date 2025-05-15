/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.execution.ScenarioExecution;
import fr.enedis.chutney.engine.domain.execution.engine.StepExecutor;
import fr.enedis.chutney.engine.domain.execution.engine.step.Step;
import fr.enedis.chutney.engine.domain.execution.report.StepExecutionReport;
import org.springframework.util.Assert;

public class RemoteStepExecutor implements StepExecutor {

    private final DelegationClient delegationClient;
    private final NamedHostAndPort agentInfo;

    public RemoteStepExecutor(DelegationClient delegationClient, NamedHostAndPort agentInfo) {
        this.delegationClient = delegationClient;
        this.agentInfo = agentInfo;
    }

    @Override
    public void execute(ScenarioExecution scenarioExecution, Target target, Step step) {
        try {
            StepExecutionReport remoteReport = delegationClient.handDown(step, agentInfo);

            guardFromIllegalReport(remoteReport);

            step.updateContextFrom(remoteReport);

            // TODO update ScenarioExecution with registered FinallyAction

        } catch (CannotDelegateException e) {
            step.failure(e);
        }
    }

    private void guardFromIllegalReport(StepExecutionReport remoteReport) {
        Assert.notNull(remoteReport.evaluatedInputs, "EvaluatedInputs are null after delegation. 0_o !");
        Assert.notNull(remoteReport.scenarioContext, "ScenarioContext is null after delegation. 0_o !");
        Assert.notNull(remoteReport.stepResults, "StepResults are null after delegation. 0_o !");
        Assert.notNull(remoteReport.status, "Status is null after delegation. 0_o !");
        Assert.notNull(remoteReport.information, "Information are null after delegation. 0_o !");
        Assert.notNull(remoteReport.errors, "Errors are null after delegation. 0_o !");
    }

}
