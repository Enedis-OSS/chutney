/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;

@SuppressWarnings("serial")
public class CannotDelegateException extends RuntimeException {

    public CannotDelegateException(NamedHostAndPort delegate) {
        super("Unable to connect to " + delegate.name() + " at " + delegate.host() + ":" + delegate.port());
    }

}
