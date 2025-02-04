/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security;

import com.chutneytesting.admin.api.InfoController;
import com.chutneytesting.security.api.SsoOpenIdConnectController;
import com.chutneytesting.security.api.UserController;
import com.chutneytesting.security.api.UserDto;
import com.chutneytesting.security.domain.AuthenticationService;
import com.chutneytesting.security.domain.Authorizations;
import com.chutneytesting.security.infra.jwt.CustomDaoAuthenticationProvider;
import com.chutneytesting.security.infra.jwt.JwtAuthenticationFilter;
import com.chutneytesting.security.infra.jwt.JwtUtil;
import com.chutneytesting.security.infra.jwt.JwtUtilPropertyConfiguration;
import com.chutneytesting.security.infra.sso.OAuth2SsoUserService;
import com.chutneytesting.security.infra.sso.OAuth2TokenAuthenticationFilter;
import com.chutneytesting.security.infra.sso.OAuth2TokenAuthenticationProvider;
import com.chutneytesting.security.infra.sso.SsoOpenIdConnectConfigProperties;
import com.chutneytesting.server.core.domain.security.Authorization;
import com.chutneytesting.server.core.domain.security.User;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ChannelSecurityConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({OAuth2AuthorizationServerProperties.class, SsoOpenIdConnectConfigProperties.class})
public class ChutneyWebSecurityConfig {

    private static final String LOGIN_URL = UserController.BASE_URL + "/login";
    private static final String LOGOUT_URL = UserController.BASE_URL + "/logout";
    private static final String API_BASE_URL_PATTERN = "/api/**";

    @Value("${management.endpoints.web.base-path:/actuator}")
    protected String actuatorBaseUrl;

    @Value("${server.ssl.enabled:true}")
    private Boolean sslEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtUtil jwtUtil(JwtUtilPropertyConfiguration jwtUtilPropertyConfiguration) {
        return new JwtUtil(jwtUtilPropertyConfiguration);
    }

    @Bean
    public AuthenticationService authenticationService(Authorizations authorizations) {
        return new AuthenticationService(authorizations);
    }

    @Bean
    public AuthenticationManager authenticationManager(List<UserDetailsService> userDetailsServices, PasswordEncoder passwordEncoder) {
        List<AuthenticationProvider> authenticationProviders = userDetailsServices.stream()
            .map(service -> createDaoAuthenticationProvider(service, passwordEncoder))
            .collect(Collectors.toList());
        return new ProviderManager(authenticationProviders);
    }

    private DaoAuthenticationProvider createDaoAuthenticationProvider(UserDetailsService service, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new CustomDaoAuthenticationProvider();
        provider.setUserDetailsService(service);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, List<UserDetailsService> userDetailsServices) {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsServices);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http, AuthenticationManager authenticationManager, AuthenticationService authenticationService, JwtAuthenticationFilter jwtAuthenticationFilter, @Nullable ClientRegistrationRepository clientRegistrationRepository, @Nullable RestOperations restOperations, JwtUtil jwtUtil) throws Exception {
        boolean enableSso = clientRegistrationRepository != null;
        if (enableSso) {
            configureSso(http, authenticationService, clientRegistrationRepository, restOperations, jwtUtil);
        }
        configureBaseHttpSecurity(http);
        UserDto anonymous = anonymous();
        http.anonymous(anonymousConfigurer -> anonymousConfigurer
                .principal(anonymous)
                .authorities(new ArrayList<>(anonymous.getAuthorities())))
            .authorizeHttpRequests(httpRequest -> {
                HandlerMappingIntrospector introspector = new HandlerMappingIntrospector();
                httpRequest
                    .requestMatchers(new MvcRequestMatcher(introspector, LOGIN_URL)).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, LOGOUT_URL)).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, InfoController.BASE_URL + "/**")).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, SsoOpenIdConnectController.BASE_URL + "/**")).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, API_BASE_URL_PATTERN)).authenticated()
                    .requestMatchers(new MvcRequestMatcher(introspector, actuatorBaseUrl + "/**")).hasAuthority(Authorization.ADMIN_ACCESS.name())
                    .anyRequest().permitAll();
            })
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(new BasicAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, enableSso ? OAuth2TokenAuthenticationFilter.class : UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    protected void configureBaseHttpSecurity(final HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .requiresChannel(this.requireChannel(sslEnabled));
    }

    protected UserDto anonymous() {
        UserDto anonymous = new UserDto();
        anonymous.setId(User.ANONYMOUS.id);
        anonymous.setName(User.ANONYMOUS.id);
        anonymous.grantAuthority("ANONYMOUS");
        return anonymous;
    }

    private Customizer<ChannelSecurityConfigurer<HttpSecurity>.ChannelRequestMatcherRegistry> requireChannel(Boolean sslEnabled) {
        if (sslEnabled) {
            return channelRequestMatcherRegistry -> channelRequestMatcherRegistry.anyRequest().requiresSecure();
        } else {
            return channelRequestMatcherRegistry -> channelRequestMatcherRegistry.anyRequest().requiresInsecure();
        }
    }

    private void configureSso(final HttpSecurity http, AuthenticationService authenticationService, ClientRegistrationRepository clientRegistrationRepository, RestOperations restOperations, JwtUtil jwtUtil) throws Exception {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new OAuth2SsoUserService(authenticationService, restOperations);
        OAuth2TokenAuthenticationProvider oAuth2TokenAuthenticationProvider = new OAuth2TokenAuthenticationProvider(oAuth2UserService, clientRegistrationRepository.findByRegistrationId("sso-provider"));
        AuthenticationManager authenticationManager = new ProviderManager(Collections.singletonList(oAuth2TokenAuthenticationProvider));
        OAuth2TokenAuthenticationFilter tokenFilter = new OAuth2TokenAuthenticationFilter(authenticationManager, jwtUtil);
        http
            .authenticationProvider(oAuth2TokenAuthenticationProvider)
            .addFilterBefore(tokenFilter, BasicAuthenticationFilter.class);
    }

    @Configuration
    @Profile("sso-auth")
    public static class SsoConfiguration {
        @Bean
        public RestOperations restOperations(SsoOpenIdConnectConfigProperties ssoOpenIdConnectConfigProperties) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            if (ssoOpenIdConnectConfigProperties.proxyHost != null && !ssoOpenIdConnectConfigProperties.proxyHost.isEmpty()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ssoOpenIdConnectConfigProperties.proxyHost, ssoOpenIdConnectConfigProperties.proxyPort));
                requestFactory.setProxy(proxy);
            }
            return new RestTemplate(requestFactory);
        }
    }
}
