/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.api;

import static fr.enedis.chutney.tokens.api.AccessTokenController.BASE_URL;

import fr.enedis.chutney.tokens.domain.AccessTokensService;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE_URL)
public class AccessTokenController {

    public static final String BASE_URL = "/api/v1/accesstokens";

    private final AccessTokensService accessTokensService;

    public AccessTokenController(AccessTokensService accessTokensService) {
        this.accessTokensService = accessTokensService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN_ACCESS','CAMPAIGN_WRITE','DATASET_WRITE','SCENARIO_WRITE','ENVIRONMENT_READ')")
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String createToken(Principal principal) {
        return accessTokensService.createToken(principal.getName(), "note", Instant.now().plus(1, ChronoUnit.MONTHS));
    }
}
