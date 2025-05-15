/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.engine.domain.environment.TargetImpl;
import fr.enedis.chutney.engine.domain.execution.engine.StepExecutor;
import java.util.List;
import java.util.Optional;

public class DelegationService {

    private final StepExecutor localStepExecutor;
    private final DelegationClient delegationClient;

    public DelegationService(StepExecutor localStepExecutor,
                             DelegationClient delegationClient) {
        this.localStepExecutor = localStepExecutor;
        this.delegationClient = delegationClient;
    }

    public StepExecutor findExecutor(Optional<Target> target) {
        if (target.isEmpty() || target.get().name().isEmpty()) {
            return localStepExecutor;
        }

        List<NamedHostAndPort> agents = ((TargetImpl) target.get()).agents;
        if (!agents.isEmpty()) {
            NamedHostAndPort nextAgent = agents.getFirst();
            // TODO should we do that here ?
            agents.removeFirst();
            return new RemoteStepExecutor(delegationClient, nextAgent);
        } else {
            return localStepExecutor;
        }
    }
}
