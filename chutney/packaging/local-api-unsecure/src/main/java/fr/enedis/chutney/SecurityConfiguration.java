/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean("unsecureFilterChain")
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        UserDto defaultUser = getDefaultUser();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .anonymous(anonymousConfigurer -> anonymousConfigurer
                .principal(defaultUser)
                .authorities(new ArrayList<>(defaultUser.getAuthorities())))
            .authorizeHttpRequests(httpRequest -> httpRequest.anyRequest().permitAll());

        http
            .requiresChannel(channelRequestMatcherRegistry -> channelRequestMatcherRegistry.anyRequest().requiresInsecure());

        return http.build();
    }

    private UserDto getDefaultUser() {
        UserDto defaultUser = new UserDto();
        defaultUser.setId("plugin");
        defaultUser.setName("ChutneyPluginUser");
        defaultUser.addRole("ADMIN");
        Arrays.stream(Authorization.values()).map(Enum::name).forEach(defaultUser::grantAuthority);
        return defaultUser;
    }
}
