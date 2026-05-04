/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.domain;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("Invalid API Key");
    }
}
