/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.scenario.api;

import static java.time.Instant.now;

import fr.enedis.chutney.scenario.api.raw.dto.GwtTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.dto.RawTestCaseDto;
import fr.enedis.chutney.scenario.api.raw.mapper.GwtTestCaseMapper;
import fr.enedis.chutney.scenario.api.raw.mapper.RawTestCaseMapper;
import fr.enedis.chutney.scenario.domain.gwt.GwtTestCase;
import fr.enedis.chutney.security.infra.SpringUserService;
import fr.enedis.chutney.server.core.domain.scenario.AggregatedRepository;
import fr.enedis.chutney.server.core.domain.scenario.ScenarioNotFoundException;
import fr.enedis.chutney.server.core.domain.scenario.TestCaseMetadataImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenario/v2")
public class GwtTestCaseController {

    private final AggregatedRepository<GwtTestCase> gwtTestCaseRepository;


    private final SpringUserService userService;

    public GwtTestCaseController(AggregatedRepository<GwtTestCase> testCaseRepository,
                                 SpringUserService userService) {
        this.gwtTestCaseRepository = testCaseRepository;
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/{testCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GwtTestCaseDto getTestCase(@PathVariable("testCaseId") String testCaseId) {
        return GwtTestCaseMapper.toDto(gwtTestCaseRepository.findById(testCaseId).orElseThrow(() -> new ScenarioNotFoundException(testCaseId)));
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String saveTestCase(@RequestBody GwtTestCaseDto testCase) {
        return saveOrUpdate(testCase);
    }

    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @PatchMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String updateTestCase(@RequestBody GwtTestCaseDto testCase) {
        return saveOrUpdate(testCase);
    }

    private String saveOrUpdate(GwtTestCaseDto testCase) {
        GwtTestCase gwtTestCase = GwtTestCaseMapper.fromDto(testCase);
        return gwtTestCaseSave(gwtTestCase);
    }

    /*
     * RAW Edition
     *
     * */
    @PreAuthorize("hasAuthority('SCENARIO_WRITE')")
    @PostMapping(path = "/raw", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String saveTestCase(@RequestBody RawTestCaseDto rawTestCaseDto) {
        GwtTestCase gwtTestCase = RawTestCaseMapper.fromDto(rawTestCaseDto);
        return gwtTestCaseSave(gwtTestCase);
    }

    @PreAuthorize("hasAuthority('SCENARIO_READ')")
    @GetMapping(path = "/raw/{testCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RawTestCaseDto getTestCaseById(@PathVariable("testCaseId") String testCaseId) {
        return RawTestCaseMapper.toDto(gwtTestCaseRepository.findById(testCaseId).orElseThrow(() -> new ScenarioNotFoundException(testCaseId)));
    }

    private String gwtTestCaseSave(GwtTestCase gwtTestCase) {
        gwtTestCase = GwtTestCase.builder()
            .withMetadata(TestCaseMetadataImpl.TestCaseMetadataBuilder.from(gwtTestCase.metadata)
                .withUpdateDate(now())
                .withAuthor(userService.currentUser().getId())
                .build())
            .withScenario(gwtTestCase.scenario)
            .build();
        return gwtTestCaseRepository.save(gwtTestCase);
    }
}
