/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.api;

import static fr.enedis.chutney.security.api.SsoOpenIdConnectMapper.toDto;

import fr.enedis.chutney.security.infra.ChutneyAuth;
import fr.enedis.chutney.security.infra.sso.SsoOpenIdConnectConfigProperties;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuthenticationConfigController.BASE_URL)
public class AuthenticationConfigController {

    public static final String BASE_URL = "/api/v1/authentication";

    private final ChutneyAuth chutneyAuth;
    private final SsoOpenIdConnectConfigProperties ssoOpenIdConnectConfigProperties;

    public AuthenticationConfigController(ChutneyAuth chutneyAuth,
                                          @Nullable SsoOpenIdConnectConfigProperties ssoOpenIdConnectConfigProperties) {
        this.chutneyAuth = chutneyAuth;
        this.ssoOpenIdConnectConfigProperties = ssoOpenIdConnectConfigProperties;
    }

    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthenticationConfigDto getAuthenticationConfig() {
        return new AuthenticationConfigDto(chutneyAuth.isEnableUserPassword(),
            chutneyAuth.isEnableSso(),
            toDto(ssoOpenIdConnectConfigProperties));
    }

}
