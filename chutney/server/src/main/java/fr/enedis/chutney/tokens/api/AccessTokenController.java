/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.api;

import static fr.enedis.chutney.tokens.api.AccessTokenController.BASE_URL;

import fr.enedis.chutney.tokens.api.dto.AccessTokenDto;
import fr.enedis.chutney.tokens.api.dto.AccessTokenRequestDto;
import fr.enedis.chutney.tokens.api.dto.CreatedAccessTokenDto;
import fr.enedis.chutney.tokens.domain.AccessTokensService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreatedAccessTokenDto createToken(Principal principal, @Valid @RequestBody AccessTokenRequestDto accessTokenRequestDto) {
        var token = accessTokensService.createToken(principal.getName(), accessTokenRequestDto.getNote(),
            accessTokenRequestDto.getExpiresAt() != null ?
                accessTokenRequestDto.getExpiresAt().atStartOfDay(ZoneId.systemDefault()).toInstant() : null
        );
        return new CreatedAccessTokenDto(accessTokenRequestDto.getNote(), token, accessTokenRequestDto.getExpiresAt());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN_ACCESS','CAMPAIGN_WRITE','DATASET_WRITE','DATASET_READ','SCENARIO_WRITE','SCENARIO_READ','ENVIRONMENT_READ')")
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AccessTokenDto> getTokensForUser(Principal principal) {
        return accessTokensService.getTokensForUser(principal.getName()).stream()
            .map(accessToken -> new AccessTokenDto(accessToken.id().toString(), accessToken.note(),
                accessToken.expiresAt() != null ? LocalDate.ofInstant(accessToken.expiresAt(), ZoneId.systemDefault()) : null))
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN_ACCESS','CAMPAIGN_WRITE','DATASET_WRITE','DATASET_READ','SCENARIO_WRITE','SCENARIO_READ','ENVIRONMENT_READ')")
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void deleteToken(Principal principal, @PathVariable("id")  String id) {
        accessTokensService.deleteTokenForUser(UUID.fromString(id), principal.getName());
    }
}
