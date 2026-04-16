/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tokens.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity(name = "ACCESS_TOKEN")
public class AccessTokenEntity implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "OWNER", updatable = false)
    private String owner;

    @Column(name = "HASHED_TOKEN", updatable = false)
    private String hashedToken;

    @Column(name = "CREATED_AT", updatable = false)
    private Long createdAt;

    public AccessTokenEntity() {
    }

    public AccessTokenEntity(Long id, String owner, String hashedToken, Long createdAt) {
        this.id = id;
        this.owner = owner;
        this.hashedToken = hashedToken;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getHashedToken() {
        return hashedToken;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}
