/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity(name = "ACCESS_TOKEN")
public class AccessTokenEntity implements Serializable {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "OWNER", updatable = false)
    private String owner;

    @Column(name = "NOTE", updatable = false)
    private String note;

    @Column(name = "HASH", updatable = false)
    private String hash;

    @Column(name = "EXPIRES_AT", updatable = false)
    private Long expiresAt;

    public AccessTokenEntity() {
    }

    public AccessTokenEntity(String id, String owner, String note, String hash, Long expiresAt) {
        this.id = id;
        this.owner = owner;
        this.note = note;
        this.hash = hash;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    public String getOwner() {
        return owner;
    }

    public String getHash() {
        return hash;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}
