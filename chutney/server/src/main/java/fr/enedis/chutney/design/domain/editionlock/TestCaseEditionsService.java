/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.domain.editionlock;

import static fr.enedis.chutney.design.domain.editionlock.TestCaseEdition.byEditor;
import static fr.enedis.chutney.design.domain.editionlock.TestCaseEdition.byId;
import static java.time.Instant.now;

import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseRepository;
import java.util.List;

public class TestCaseEditionsService {

  private final TestCaseEditions testCaseEditions;
  private final TestCaseRepository testCaseRepository;

  public TestCaseEditionsService(TestCaseEditions testCaseEditions, TestCaseRepository testCaseRepository) {
    this.testCaseEditions = testCaseEditions;
    this.testCaseRepository = testCaseRepository;
  }

    public List<TestCaseEdition> getTestCaseEditions(String testCaseId) {
        return testCaseEditions.findBy(byId(testCaseId));
    }

    public TestCaseEdition editTestCase(String testCaseId, String user) {
        List<TestCaseEdition> edition = testCaseEditions.findBy(byId(testCaseId).and(byEditor(user)));
        if (!edition.isEmpty()) {
            return edition.getFirst();
        }

        TestCaseEdition testCaseEdition = new TestCaseEdition(
            testCaseRepository.findById(testCaseId).orElseThrow(() -> new ScenarioNotFoundException(testCaseId)).metadata(),
            now(),
            user
        );

        if (testCaseEditions.add(testCaseEdition)) {
            return testCaseEdition;
        }

        throw new IllegalStateException("Cannot lock scenario edition");
    }

    public void endTestCaseEdition(String testCaseId, String user) {
        testCaseEditions.findBy(byId(testCaseId).and(byEditor(user)))
            .forEach(testCaseEditions::remove);
    }
}
