/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security;

import static fr.enedis.chutney.config.ServerConfigurationValues.SERVER_PORT_SPRING_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.nimbusds.jose.JOSEException;
import fr.enedis.chutney.admin.api.InfoController;
import fr.enedis.chutney.security.api.SsoOpenIdConnectController;
import fr.enedis.chutney.security.api.UserController;
import fr.enedis.chutney.security.api.UserDto;
import fr.enedis.chutney.security.domain.AuthenticationService;
import fr.enedis.chutney.security.infra.jwt.ChutneyJwtAuthenticationConverter;
import fr.enedis.chutney.security.infra.jwt.ChutneyJwtProperties;
import fr.enedis.chutney.security.infra.jwt.JwtUtil;
import fr.enedis.chutney.security.infra.sso.OAuth2TokenAuthenticationFilter;
import fr.enedis.chutney.security.infra.sso.SsoOpenIdConnectConfigProperties;
import fr.enedis.chutney.server.core.domain.security.Authorization;
import fr.enedis.chutney.server.core.domain.security.User;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ChannelSecurityConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({OAuth2AuthorizationServerProperties.class, SsoOpenIdConnectConfigProperties.class})
@ConditionalOnProperty(value = "chutney.security.enabled", havingValue = "true", matchIfMissing = true)
public class ChutneyWebSecurityConfig {

    private static final String LOGIN_URL = UserController.BASE_URL + "/login";
    private static final String API_BASE_URL_PATTERN = "/api/**";

    @Value("${management.endpoints.web.base-path:/actuator}")
    protected String actuatorBaseUrl;

    @Value("${server.ssl.enabled:true}")
    private Boolean sslEnabled;

    @Bean
    public JwtUtil jwtUtil(ChutneyJwtProperties chutneyJwtProperties) throws JOSEException {
        return new JwtUtil(chutneyJwtProperties);
    }

    @Bean
    public OAuth2TokenAuthenticationFilter oAuth2TokenAuthenticationFilter(JwtUtil jwtUtil, AuthenticationService authenticationService) {
        return new OAuth2TokenAuthenticationFilter(jwtUtil, authenticationService);
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtUtil jwtUtil) throws JOSEException {
        return jwtUtil.nimbusJwtDecoder();
    }

    @Bean
    ChutneyJwtAuthenticationConverter chutneyJwtAuthenticationConverter(AuthenticationService authenticationService) {
        return new ChutneyJwtAuthenticationConverter(authenticationService);
    }

    @Bean
    AuthenticationManagerResolver<HttpServletRequest> tokenAuthenticationManagerResolver(JwtDecoder jwtDecoder, @Nullable() OpaqueTokenIntrospector opaqueTokenIntrospector, ChutneyJwtAuthenticationConverter jwtConverter, RestOperations restOperations) {
        var jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        jwtAuthenticationProvider.setJwtAuthenticationConverter(jwtConverter);
        OpaqueTokenIntrospector opaqueTokenIntrospectorFinal = opaqueTokenIntrospector != null
            ? opaqueTokenIntrospector
            : (String token) -> new DefaultOAuth2AuthenticatedPrincipal(emptyMap(), emptyList());
        AuthenticationManager jwt = new ProviderManager(jwtAuthenticationProvider);
        AuthenticationManager opaqueToken = new ProviderManager(
            new OpaqueTokenAuthenticationProvider(opaqueTokenIntrospectorFinal));
        return (request) -> useJwt(request) ? jwt : opaqueToken;
    }

    private boolean useJwt(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return token.split("\\.").length == 3;
        }
        return false;
    }

    @Bean
    @ConfigurationProperties(prefix = "chutney.security.cors")
    CorsConfiguration corsConfiguration( @Value(SERVER_PORT_SPRING_VALUE) int serverPort) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://localhost:"+serverPort));
        config.setAllowedMethods(Arrays.stream(HttpMethod.values()).map(HttpMethod::name).toList());
        config.setAllowedHeaders(List.of(CorsConfiguration.ALL));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(CorsConfiguration corsConfiguration) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http, AuthenticationManagerResolver<HttpServletRequest> tokenAuthenticationManagerResolver, OAuth2TokenAuthenticationFilter oAuth2TokenAuthenticationFilter, JwtUtil jwtUtil, CorsConfigurationSource corsConfigurationSource) throws Exception {
        configureBaseHttpSecurity(http);
        UserDto anonymous = anonymous();
        http
            .cors(Customizer.withDefaults())
            .anonymous(anonymousConfigurer -> anonymousConfigurer
                .principal(anonymous)
                .authorities(new ArrayList<>(anonymous.getAuthorities())))
            .authorizeHttpRequests(httpRequest -> {
                HandlerMappingIntrospector introspector = new HandlerMappingIntrospector();
                httpRequest
                    .requestMatchers(new MvcRequestMatcher(introspector, LOGIN_URL)).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, InfoController.BASE_URL + "/**")).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, SsoOpenIdConnectController.BASE_URL + "/**")).permitAll()
                    .requestMatchers(new MvcRequestMatcher(introspector, API_BASE_URL_PATTERN)).authenticated()
                    .requestMatchers(new MvcRequestMatcher(introspector, actuatorBaseUrl + "/**")).hasAuthority(Authorization.ADMIN_ACCESS.name())
                    .anyRequest().permitAll();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(tokenAuthenticationManagerResolver)
            )
            .formLogin(httpSecurityFormLoginConfigurer -> httpSecurityFormLoginConfigurer
                .loginProcessingUrl(LOGIN_URL)
                .successHandler(new HttpLoginSuccessHandler(jwtUtil))
                .failureHandler(new HttpLoginFailureHandler())
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(new CorsFilter(corsConfigurationSource), BearerTokenAuthenticationFilter.class)
            .addFilterAfter(oAuth2TokenAuthenticationFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private void configureBaseHttpSecurity(final HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .requiresChannel(this.requireChannel(sslEnabled));
    }

    private UserDto anonymous() {
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
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(ssoOpenIdConnectConfigProperties.clientId, ssoOpenIdConnectConfigProperties.clientSecret));
            return restTemplate;
        }

        @Bean
        @Primary
        @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
        public OpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2ResourceServerProperties properties, RestOperations restOperations) {
            return new SpringOpaqueTokenIntrospector(properties.getOpaquetoken().getIntrospectionUri(), restOperations);
        }
    }
}
