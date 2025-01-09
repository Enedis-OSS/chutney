/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.scenario.infra.aop;

import com.chutneytesting.scenario.domain.gwt.GwtTestCase;
import com.chutneytesting.scenario.infra.index.ScenarioIndexRepository;
import com.chutneytesting.scenario.infra.jpa.ScenarioEntity;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScenarioIndexingAspect {
    private final ScenarioIndexRepository scenarioIndexRepository;

    public ScenarioIndexingAspect(ScenarioIndexRepository scenarioIndexRepository) {
        this.scenarioIndexRepository = scenarioIndexRepository;
    }



    @AfterReturning(
        pointcut = "execution(* com.chutneytesting.scenario.infra.raw.DatabaseTestCaseRepository.save(..)) && args(testCase)",
        returning = "id",
        argNames = "testCase,id")
    public void index(GwtTestCase testCase, String id) {
        TestCaseMetadataImpl testCaseMetadata = TestCaseMetadataImpl.TestCaseMetadataBuilder.from(testCase.metadata()).withId(id).build();
        GwtTestCase testCaseWithId = GwtTestCase.builder().from(testCase).withMetadata(testCaseMetadata).build();
        scenarioIndexRepository.save(ScenarioEntity.fromGwtTestCase(testCaseWithId));
    }

    @After("execution(* com.chutneytesting.scenario.infra.raw.DatabaseTestCaseRepository.removeById(..)) && args(scenarioId)")
    public void delete(String scenarioId) {
        scenarioIndexRepository.delete(scenarioId);

    }
}
