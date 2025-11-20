/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.api.editionlock;

import static java.util.stream.Collectors.toList;

import fr.enedis.chutney.design.domain.editionlock.TestCaseEdition;
import fr.enedis.chutney.design.domain.editionlock.TestCaseEditionsService;
import fr.enedis.chutney.security.infra.SpringUserService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(TestCaseEditionController.BASE_URL)
public class TestCaseEditionController {

    public static final String BASE_URL = "/api/v1/editions/testcases";

    private final TestCaseEditionsService testCaseEditionsService;
    private final SpringUserService userService;

    public TestCaseEditionController(TestCaseEditionsService testCaseEditionsService, SpringUserService userService) {
        this.testCaseEditionsService = testCaseEditionsService;
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/{testCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TestCaseEditionDto> testCasesEditions(@PathVariable("testCaseId") String testCaseId) {
        return testCaseEditionsService.getTestCaseEditions(testCaseId).stream()
            .map(TestCaseEditionController::toDto)
            .collect(toList());
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @PostMapping(path = "/{testCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TestCaseEditionDto editTestCase(@PathVariable("testCaseId") String testCaseId) {
        return toDto(testCaseEditionsService.editTestCase(testCaseId, userService.currentUserId()));
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @DeleteMapping(path = "/{testCaseId}")
    public void endTestCaseEdition(@PathVariable("testCaseId") String testCaseId) {
        testCaseEditionsService.endTestCaseEdition(testCaseId, userService.currentUserId());
    }

    private static TestCaseEditionDto toDto(TestCaseEdition tcEdition) {
        return ImmutableTestCaseEditionDto.builder()
            .testCaseId(tcEdition.testCaseMetadata.id())
            .testCaseVersion(tcEdition.testCaseMetadata.version())
            .editionUser(tcEdition.editor)
            .editionStartDate(tcEdition.startDate)
            .build();
    }
}
