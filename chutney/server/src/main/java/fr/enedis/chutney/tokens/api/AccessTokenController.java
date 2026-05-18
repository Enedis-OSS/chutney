/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.api;

import static fr.enedis.chutney.tokens.api.AccessTokenController.BASE_URL;

import fr.enedis.chutney.tokens.api.dto.AccessTokenDto;
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PreAuthorize("hasAnyAuthority('ADMIN_ACCESS','CAMPAIGN_WRITE','DATASET_WRITE','DATASET_READ','SCENARIO_WRITE','SCENARIO_READ','ENVIRONMENT_READ')")
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String createToken(Principal principal, @Valid @RequestBody AccessTokenDto accessTokenDto) {
        return accessTokensService.createToken(principal.getName(), accessTokenDto.getNote(),
            accessTokenDto.getExpiresAt() != null ?
                accessTokenDto.getExpiresAt().atStartOfDay(ZoneId.systemDefault()).toInstant() : null
        );
    }

    @PreAuthorize("hasAnyAuthority('ADMIN_ACCESS','CAMPAIGN_WRITE','DATASET_WRITE','DATASET_READ','SCENARIO_WRITE','SCENARIO_READ','ENVIRONMENT_READ')")
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AccessTokenDto> getTokensForUser(Principal principal) {
        return accessTokensService.getTokensForUser(principal.getName()).stream()
            .map(accessToken -> new AccessTokenDto(accessToken.note(),
                accessToken.expiresAt() != null ? LocalDate.ofInstant(accessToken.expiresAt(), ZoneId.systemDefault()) : null))
            .collect(Collectors.toList());
    }
}
