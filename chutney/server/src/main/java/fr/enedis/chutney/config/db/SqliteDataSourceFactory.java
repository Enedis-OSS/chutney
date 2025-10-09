/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.config.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.sqlite.SQLiteConfig;

class SqliteDataSourceFactory {

    static DataSource dataSource(
        String jdbcUrl,
        boolean readOnly,
        @Nullable  String tmpDirectoryOrNull
    ) {
        SQLiteConfig config = config(readOnly, tmpDirectoryOrNull);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(false);
        hikariConfig.setReadOnly(readOnly);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setDriverClassName(DatabaseDriver.fromJdbcUrl(jdbcUrl).getDriverClassName());
        hikariConfig.setMaximumPoolSize(1); // fix for sqlite

        hikariConfig.setDataSourceProperties(config.toProperties());
        return new HikariDataSource(hikariConfig);
    }

    private static SQLiteConfig config(boolean readOnly, String tmpDirectoryOrNull) {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(readOnly);
        if (!readOnly) {
            // See https://www.sqlite.org/pragma.html
            config.setLockingMode(SQLiteConfig.LockingMode.NORMAL);
            config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
            config.setDefaultCacheSize(-20000);   // ~20MB
            config.setPageSize(4096);              // 4KB
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            config.setBusyTimeout(10000);
            if (StringUtils.isNotBlank(tmpDirectoryOrNull)) {
                config.setTempStoreDirectory(tmpDirectoryOrNull);
            }
        }

        return config;
    }
}
