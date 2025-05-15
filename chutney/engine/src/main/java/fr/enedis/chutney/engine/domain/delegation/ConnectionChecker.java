/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.engine.domain.delegation;

/**
 * Used to determine if a remote port at a remote address is listened to.
 */
public interface ConnectionChecker {

    boolean canConnectTo(NamedHostAndPort namedHostAndPort);
}
