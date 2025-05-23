/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.security.domain;

public class CurrentUserNotFoundException extends RuntimeException {
    public CurrentUserNotFoundException() {
        super("Current user could not be found");
    }
}
