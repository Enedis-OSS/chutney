/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;

public class CreatedAccessTokenDto {

    private String id;

    private String note;

    private String token;

    private LocalDate expiresAt;

    @JsonCreator
    public CreatedAccessTokenDto() {
    }

    public CreatedAccessTokenDto(String id, String note, String token, LocalDate expiresAt) {
        this.id = id;
        this.note = note;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    public String getToken() {
        return token;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }
}
