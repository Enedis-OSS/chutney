/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra.memory;

import fr.enedis.chutney.security.domain.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("mem-auth")
public class InMemorySecurityConfiguration {

    @Bean
    @ConfigurationProperties("chutney.security")
    public InMemoryUsersProperties users() {
        return new InMemoryUsersProperties();
    }

    @Bean
    public InMemoryUserDetailsService inMemoryUserDetailsService(InMemoryUsersProperties users, AuthenticationService authenticationService) {
        return new InMemoryUserDetailsService(users, authenticationService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Configuration
    @Profile("mem-auth")
    static class InMemoryConfiguration {

        @Autowired
        protected void configure(
            final AuthenticationManagerBuilder auth, final InMemoryUserDetailsService userDetailsService) throws Exception {
            auth.userDetailsService(userDetailsService);
        }
    }
}
