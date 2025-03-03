/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.security.infra;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.List;

public class CompositeUserDetailService implements UserDetailsService {

    private final List<UserDetailsService> userDetailsServices;

    public CompositeUserDetailService(List<UserDetailsService> userDetailsServices) {
        this.userDetailsServices = userDetailsServices;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        for (UserDetailsService service : userDetailsServices) {
            try {
                return service.loadUserByUsername(username);
            } catch (UsernameNotFoundException ignored) {}
        }
        throw new UsernameNotFoundException("User not found in any service: " + username);
    }
}
