/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessTokenDto {

    private String note;
    private LocalDate expiresAt;

    @JsonCreator
    public AccessTokenDto() {
    }

    public AccessTokenDto(String note, LocalDate expiresAt) {
        this.note = note;
        this.expiresAt = expiresAt;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }
}
