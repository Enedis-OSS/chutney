/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.infra;

import static java.util.Optional.ofNullable;

import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.TestCase;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadata;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class TestCaseRepositoryAggregator implements TestCaseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseRepositoryAggregator.class);
    private final List<AggregatedRepository<? extends TestCase>> aggregatedRepositories;

    public TestCaseRepositoryAggregator(List<AggregatedRepository<? extends TestCase>> aggregatedRepositories) {
        this.aggregatedRepositories = aggregatedRepositories;
    }

    @Override
    public String save(TestCase scenario) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TestCase> findById(String testCaseId) {
        return aggregatedRepositories
            .stream()
            .map(repo -> {
                try {
                    return repo.findById(testCaseId);
                } catch (Exception e) {
                    return Optional.empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(opt -> (TestCase) opt)
            .findFirst();
    }

    @Override
    public Optional<TestCase> findExecutableById(String testCaseId) {
        return aggregatedRepositories
            .stream()
            .map(repo -> {
                try {
                    return repo.findExecutableById(testCaseId);
                } catch (Exception e) {
                    return Optional.empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(opt -> (TestCase) opt)
            .findFirst();
    }

    @Override
    public Optional<TestCaseMetadata> findMetadataById(String testCaseId) {
        Optional<TestCase> testCase = findById(testCaseId);
        return testCase.map(TestCase::metadata);
    }

    @Override
    public List<TestCaseMetadata> findAll() {
        return aggregatedRepositories
            .stream()
            .parallel()
            .flatMap(r ->
                getTestCaseMetadataStream(r::findAll, r.getClass().getSimpleName())
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<TestCaseMetadata> findAllByDatasetId(String datasetId) {
        return aggregatedRepositories
            .stream()
            .flatMap(r ->
                getTestCaseMetadataStream(r::findAll, r.getClass().getSimpleName())
            )
            .filter(testCase ->
                ofNullable(testCase.defaultDataset())
                    .map(t -> t.equals(datasetId))
                    .orElse(datasetId == null))
            .collect(Collectors.toList());
    }

    @Override
    public void removeById(String testCaseId) {
        aggregatedRepositories.forEach(repo -> {
                try {
                    repo.removeById(testCaseId);
                } catch (Exception e) {
                    // no-op
                }
        });
    }

    @Override
    public Optional<Integer> lastVersion(String testCaseId) {
        Optional<AggregatedRepository<? extends TestCase>> repository = aggregatedRepositories
            .stream()
            .filter(repo -> repo.findById(testCaseId).isPresent())
            .findFirst();
        if (repository.isPresent()) {
            return repository.get().lastVersion(testCaseId);
        } else {
            throw new ScenarioNotFoundException(testCaseId);
        }
    }

    private Stream<TestCaseMetadata> getTestCaseMetadataStream(Supplier<List<TestCaseMetadata>> sup, String repoName) {
        try {
            return sup.get().stream();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not search scenarios from repository {}", repoName, e);
            return Stream.empty();
        }
    }
}
