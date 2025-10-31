/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.api;

import static fr.enedis.chutney.dataset.api.DataSetMapper.fromDto;
import static fr.enedis.chutney.dataset.api.DataSetMapper.toDto;

import fr.enedis.chutney.dataset.domain.DatasetService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DataSetController.BASE_URL)
public class DataSetController {

    public static final String BASE_URL = "/api/v1/datasets";

    private final DatasetService datasetService;

    public DataSetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PreAuthorize("hasAuthority('DATASET_READ') or hasAuthority('SCENARIO_READ') or hasAuthority('CAMPAIGN_READ') or hasAuthority('EXECUTION_WRITE')")
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataSetDto> findAll(@RequestParam("usage") Optional<Boolean> query) {
        return datasetService.findAll(query.orElse(false))
            .stream()
            .map(DataSetMapper::toDto)
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('DATASET_WRITE')")
    @PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataSetDto save(@RequestBody DataSetDto datasetDto) {
        hasNoDuplicatedHeaders(datasetDto);
        return toDto(datasetService.save(fromDto(datasetDto)));
    }

    @PreAuthorize("hasAuthority('DATASET_WRITE')")
    @PutMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataSetDto update(@RequestBody DataSetDto dataSetDto, @RequestParam Optional<String> oldId) {
        hasNoDuplicatedHeaders(dataSetDto);
        if (oldId.isPresent()) {
            return toDto(datasetService.updateWithRename(oldId.get(), fromDto(dataSetDto)));
        } else {
            return save(dataSetDto);
        }
    }

    @PreAuthorize("hasAuthority('DATASET_WRITE')")
    @DeleteMapping(path = "/{datasetName}")
    public void deleteById(@PathVariable String datasetName) {
        datasetService.remove(datasetName);
    }

    @PreAuthorize("hasAuthority('DATASET_READ') or hasAuthority('EXECUTION_WRITE')")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataSetDto findById(@PathVariable String id) {
        return toDto(datasetService.findById(id));
    }

    static void hasNoDuplicatedHeaders(DataSetDto dataset) {
        List<String> duplicates = dataset.duplicatedHeaders();
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(duplicates.size() + " column(s) have duplicated headers: [" + String.join(", ", duplicates) + "]");
        }
    }

}
