/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.api.plugins.linkifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableLinkifierDto.class)
@Value.Style(jdkOnly = true)
public interface LinkifierDto {

    @JsonProperty("pattern")
    String pattern();

    @JsonProperty("link")
    String link();

    @JsonProperty("id")
    String id();

    @JsonCreator
    static LinkifierDto of(
        @JsonProperty("pattern") String pattern,
        @JsonProperty("link") String link,
        @JsonProperty("id") String id
    ) {
        return ImmutableLinkifierDto.builder()
            .pattern(pattern)
            .link(link)
            .id(id)
            .build();
    }
}
