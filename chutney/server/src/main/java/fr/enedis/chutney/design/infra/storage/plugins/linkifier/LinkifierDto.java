/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.design.infra.storage.plugins.linkifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkifierDto {
    public String pattern;
    public String link;

    @JsonCreator
    public LinkifierDto(
        @JsonProperty("pattern") String pattern,
        @JsonProperty("link") String link
    ) {
        this.pattern = pattern != null ? pattern : "";
        this.link = link != null ? link : "";
    }
}
