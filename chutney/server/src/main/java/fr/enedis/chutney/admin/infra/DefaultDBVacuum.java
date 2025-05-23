/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.infra;

import static java.util.Collections.emptyMap;

import fr.enedis.chutney.admin.domain.DBVacuum;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DefaultDBVacuum implements DBVacuum {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDBVacuum.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSourceProperties dsProperties;

    public DefaultDBVacuum(
        NamedParameterJdbcTemplate jdbcTemplate,
        DataSourceProperties dsProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dsProperties = dsProperties;
    }

    @Override
    public VacuumReport vacuum() {
        var dbBeforeSize = size();
        LOGGER.info("Vacuum start [{}]", dbBeforeSize);
        switch (JDBCDriver.valueFromJDBCUrl(dsProperties.determineUrl())) {
            case SQLITE -> {
                try (Connection conn = Objects.requireNonNull(jdbcTemplate.getJdbcTemplate().getDataSource()).getConnection()) {
                    conn.setAutoCommit(true);
                    conn.createStatement().execute("VACUUM");
                } catch (Exception e) {
                    LOGGER.error("Error vacuuming", e);
                }
            }
            case POSTGRES, H2 ->
                throw new UnsupportedOperationException("Database Vacuum is only supported for SQLite database");
        }
        var dbAfterSize = size();
        LOGGER.info("Vacuum end [{}]", dbAfterSize);
        return new VacuumReport(dbBeforeSize, dbAfterSize);
    }

    @Override
    public long size() {
        switch (JDBCDriver.valueFromJDBCUrl(dsProperties.determineUrl())) {
            case SQLITE -> {
                Map<String, Object> result = jdbcTemplate.queryForMap("select page_size * page_count from pragma_page_count(), pragma_page_size()", emptyMap());
                Object size = result.values().stream().findFirst().get();
                if (size instanceof Integer ri) {
                    return ri.longValue();
                }
                if (size instanceof Long rl) {
                    return rl;
                }
            }
            case POSTGRES -> {
                Map<String, Object> result = jdbcTemplate.queryForMap("select pg_database_size(current_database())", emptyMap());
                return (Long) result.values().stream().findFirst().get();
            }
            case H2 -> {
                if (dsProperties.determineUrl().startsWith("jdbc:h2:mem")) {
                    Map<String, Object> result = jdbcTemplate.queryForMap("select memory_used()", emptyMap());
                    Long size = (Long) result.values().stream().findFirst().get();
                    return 1024 * size;
                } else {
                    Map<String, Object> result = jdbcTemplate.queryForMap("select setting_value from information_schema.settings where setting_name = 'info.FILE_SIZE'", emptyMap());
                    return Long.parseLong(result.values().stream().findFirst().get().toString());
                }
            }
        }
        throw new UnsupportedOperationException("Database size computation is only supported for SQLite, PostGreSQL and H2 databases");
    }

    private enum JDBCDriver {
        SQLITE("sqlite"),
        POSTGRES("postgresql"),
        H2("h2");

        final String driverName;

        JDBCDriver(String jdbcUrlDriverName) {
            this.driverName = jdbcUrlDriverName;
        }

        static JDBCDriver valueFromJDBCUrl(String jdbcUrl) {
            for (JDBCDriver e : JDBCDriver.values()) {
                if (e.driverName.equals(jdbcUrl.split(":")[1])) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Cannot find a supported driver from url " + jdbcUrl);
        }
    }
}
