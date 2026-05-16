/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.jira.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableJiraConfigurationDto.class)
@Value.Style(jdkOnly = true)
public interface JiraConfigurationDto {

    @JsonProperty("url")
    String url();

    @JsonProperty("username")
    String username();

    @JsonProperty("password")
    String password();

    @JsonProperty("urlProxy")
    Optional<String> urlProxy();

    @JsonProperty("userProxy")
    Optional<String> userProxy();

    @JsonProperty("passwordProxy")
    Optional<String> passwordProxy();

    @JsonCreator
    static JiraConfigurationDto of(
        @JsonProperty("url") String url,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("urlProxy") @Nullable String urlProxy,
        @JsonProperty("userProxy") @Nullable String userProxy,
        @JsonProperty("passwordProxy") @Nullable String passwordProxy
    ) {
        ImmutableJiraConfigurationDto.Builder builder = ImmutableJiraConfigurationDto.builder()
            .url(url)
            .username(username)
            .password(password);
        if (urlProxy != null) builder.urlProxy(urlProxy);
        if (userProxy != null) builder.userProxy(userProxy);
        if (passwordProxy != null) builder.passwordProxy(passwordProxy);
        return builder.build();
    }
}
