/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.api;

import static java.util.stream.Collectors.toSet;

import fr.enedis.chutney.index.api.dto.Hit;
import fr.enedis.chutney.index.domain.IndexObject;
import fr.enedis.chutney.index.domain.IndexService;
import fr.enedis.chutney.server.core.domain.security.UserService;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final IndexService indexService;
    private final UserService userService;

    public SearchController(IndexService indexService, UserService userService) {
        this.indexService = indexService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCENARIO_READ', 'CAMPAIGN_READ', 'DATASET_READ')")
    public List<Hit> search(@RequestParam("keyword") String keyword) {
        var requestedObjects = userService.currentUserAuthorizations().stream()
            .map(IndexObject::fromAuthorization)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());

        return indexService.search(keyword, requestedObjects);
    }
}
