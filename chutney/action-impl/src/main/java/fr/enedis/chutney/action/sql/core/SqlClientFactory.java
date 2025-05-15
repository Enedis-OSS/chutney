/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.sql.core;


import fr.enedis.chutney.action.spi.injectable.Target;

public interface SqlClientFactory {

    SqlClient create(Target target);

}
