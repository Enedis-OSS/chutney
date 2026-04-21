/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.infra;

import fr.enedis.chutney.scenario.api.GwtTestCaseController;
import fr.enedis.chutney.security.domain.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class ApiAuthenticationFilter extends GenericFilterBean {

    private static final Collection<String> API_PATHS = List.of(
        GwtTestCaseController.BASE_URL + "/raw");

    private final AuthenticationService authenticationService;

    public ApiAuthenticationFilter(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
        throws IOException {
        String requestURI = ((HttpServletRequest) request).getRequestURI();
        try {
            if(API_PATHS.contains(requestURI)) {
                Authentication authentication = authenticationService.getAuthentication((HttpServletRequest) request);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            var printWriter = httpResponse.getWriter();
            printWriter.print(e.getMessage());
            printWriter.flush();
            printWriter.close();
        }
    }
}
