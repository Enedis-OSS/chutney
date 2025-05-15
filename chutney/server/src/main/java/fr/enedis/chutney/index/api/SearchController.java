/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.api;

import fr.enedis.chutney.index.api.dto.Hit;
import fr.enedis.chutney.index.domain.IndexService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final IndexService indexService;

    public SearchController(IndexService indexService) {
        this.indexService = indexService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCENARIO_READ', 'CAMPAIGN_READ', 'DATASET_READ')")
    public List<Hit> search(@RequestParam("keyword") String keyword) {
        return indexService.search(keyword);
    }
}
