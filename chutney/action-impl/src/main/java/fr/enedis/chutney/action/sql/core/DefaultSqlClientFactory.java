/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.sql.core;

import fr.enedis.chutney.action.spi.injectable.Target;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;

public class DefaultSqlClientFactory implements SqlClientFactory {

    private final int DEFAULT_MAX_FETCH_SIZE = 1000;

    @Override
    public SqlClient create(Target target, int minimumMemoryPercentageRequired) {
        return this.doCreate(target, minimumMemoryPercentageRequired);
    }

    private SqlClient doCreate(Target target, int minimumMemoryPercentageRequired) {
        Properties props = new Properties();
        props.put("jdbcUrl", target.property("jdbcUrl").orElse(target.uri().toString()));
        target.user().ifPresent(user -> props.put("username", user));
        target.userPassword().ifPresent(password -> props.put("password", password));

        props.putAll(target.prefixedProperties("dataSource."));
        final HikariConfig config = new HikariConfig(props);
        final HikariDataSource ds = new HikariDataSource(config);

        return new SqlClient(
            ds,
            target.numericProperty("maxFetchSize").map(Number::intValue).orElse(DEFAULT_MAX_FETCH_SIZE),
            minimumMemoryPercentageRequired
        );
    }
}
