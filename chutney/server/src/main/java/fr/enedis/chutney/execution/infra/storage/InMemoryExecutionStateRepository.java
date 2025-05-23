/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.storage;

import fr.enedis.chutney.server.core.domain.execution.state.ExecutionStateRepository;
import fr.enedis.chutney.server.core.domain.execution.state.RunningScenarioState;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryExecutionStateRepository implements ExecutionStateRepository {

    private final Map<String, RunningScenarioState> internalStorage = new ConcurrentHashMap<>();

    @Override
    public Set<RunningScenarioState> runningScenarios() {
        return new HashSet<>(internalStorage.values());
    }

    @Override
    public void notifyExecutionStart(final String scenarioId) {
        internalStorage.put(scenarioId, RunningScenarioState.of(scenarioId));
    }

    @Override
    public void notifyExecutionEnd(String scenarioId) {
        internalStorage.remove(scenarioId);
    }

    @Override
    public Optional<RunningScenarioState> runningState(String scenarioId) {
        return Optional.ofNullable(internalStorage.get(scenarioId));
    }
}
