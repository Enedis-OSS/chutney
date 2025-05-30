/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.infra.aop;

import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.scenario.infra.index.ScenarioIndexRepository;
import fr.enedis.chutney.scenario.infra.jpa.ScenarioEntity;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScenarioIndexingAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioIndexingAspect.class);
    private final ScenarioIndexRepository scenarioIndexRepository;

    public ScenarioIndexingAspect(ScenarioIndexRepository scenarioIndexRepository) {
        this.scenarioIndexRepository = scenarioIndexRepository;
    }

    @AfterReturning(
        pointcut = "execution(* fr.enedis.chutney.scenario.infra.raw.DatabaseTestCaseRepository.save(..)) && args(testCase)",
        returning = "id",
        argNames = "testCase,id")
    public void index(GwtTestCase testCase, String id) {
        try {
            TestCaseMetadataImpl testCaseMetadata = TestCaseMetadataImpl.TestCaseMetadataBuilder.from(testCase.metadata()).withId(id).build();
            GwtTestCase testCaseWithId = GwtTestCase.builder().from(testCase).withMetadata(testCaseMetadata).build();
            scenarioIndexRepository.save(ScenarioEntity.fromGwtTestCase(testCaseWithId));
        } catch (Exception e) {
            LOGGER.error("Error when indexing scenario: ", e);
        }
    }

    @After("execution(* fr.enedis.chutney.scenario.infra.raw.DatabaseTestCaseRepository.removeById(..)) && args(scenarioId)")
    public void delete(String scenarioId) {
        try {
            scenarioIndexRepository.delete(scenarioId);
        } catch (Exception e) {
            LOGGER.error("Error when deleting scenario index: ", e);
        }
    }
}
