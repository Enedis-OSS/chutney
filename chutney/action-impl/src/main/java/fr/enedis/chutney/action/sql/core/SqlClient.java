/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.sql.core;

import static fr.enedis.chutney.tools.ChutneyMemoryInfo.hasEnoughAvailableMemory;
import static fr.enedis.chutney.tools.ChutneyMemoryInfo.maxMemory;
import static fr.enedis.chutney.tools.ChutneyMemoryInfo.usedMemory;
import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

import fr.enedis.chutney.tools.NotEnoughMemoryException;
import com.zaxxer.hikari.HikariDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlClient {

    private final HikariDataSource dataSource;
    private final int maxFetchSize;
    private final int minimumMemoryPercentageRequired;

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlClient.class);

    public SqlClient(HikariDataSource dataSource, int maxFetchSize, int minimumMemoryPercentageRequired) {
        this.dataSource = dataSource;
        this.maxFetchSize = maxFetchSize;
        this.minimumMemoryPercentageRequired = minimumMemoryPercentageRequired;
    }

    public Records execute(String query) throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (final Statement statement = connection.createStatement()) {
                statement.setFetchSize(maxFetchSize);
                statement.execute(query);
                var converter = new StatementConverter(minimumMemoryPercentageRequired);
                return converter.createRecords(statement);
            }
        } finally {
            silentClose(connection);
        }
    }

    public void closeDatasource() {
        this.dataSource.close();
    }

    public Records emptyRecords() {
        return new Records(0, Collections.emptyList(), Collections.emptyList());
    }

    private void silentClose(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                //Silent close
            }
        }
    }

    private static class StatementConverter {

        private final int minimumMemoryPercentageRequired;

        private StatementConverter(int minimumMemoryPercentageRequired) {
            this.minimumMemoryPercentageRequired = minimumMemoryPercentageRequired;
        }

        private Records createRecords(Statement statement) throws SQLException {
            final int affectedRows = statement.getUpdateCount();
            List<Column> columns = Collections.emptyList();
            List<Row> rows = Collections.emptyList();

            if (isSelectQuery(affectedRows)) {
                try (final ResultSet rs = statement.getResultSet()) {
                    final ResultSetMetaData md = rs.getMetaData();

                    columns = createHeaders(md, md.getColumnCount());
                    rows = createRows(rs, columns, md.getColumnCount());
                }
            }

            return new Records(affectedRows, columns, rows);
        }

        private boolean isSelectQuery(int affectedRows) {
            return affectedRows == -1;
        }

        private List<Column> createHeaders(ResultSetMetaData md, int columnCount) throws SQLException {
            final var headers = new ArrayList<Column>(columnCount);
            int j = 0;
            for (int i = 1; i <= columnCount; i++) {
                headers.add(new Column(md.getColumnLabel(i), j++));
            }
            return headers;
        }

        private List<Row> createRows(ResultSet rs, List<Column> columns, int columnCount) throws SQLException {
            final var rows = new ArrayList<Row>();
            int j = 0;
            while (rs.next()) {
                if (j++ > 100000) {
                    throw new NonOptimizedQueryException();
                }

                if (minimumMemoryPercentageRequired > 0 && !hasEnoughAvailableMemory(minimumMemoryPercentageRequired)) {
                    throw new NotEnoughMemoryException(usedMemory(), maxMemory(), "Query fetched " + rows.size() + " rows");
                }

                final List<Cell> cells = new ArrayList<>(columnCount);
                int columnIndex = 0;
                for (int i = 1; i <= columnCount; i++) {
                    cells.add(new Cell(columns.get(columnIndex++), boxed(rs, i)));
                }
                rows.add(new Row(cells));
            }
            return rows;
        }

        private Object boxed(ResultSet rs, int i) throws SQLException {
            Object o = rs.getObject(i);
            Class<?> type = o == null ? Object.class : o.getClass();
            if (isPrimitiveOrWrapper(type) || isJDBCNumericType(type) || isJDBCDateType(type)) {
                return o;
            }
            if (o instanceof Blob) {
                return readBlob((Blob) o);
            }

            return String.valueOf(rs.getString(i));
        }

        private boolean isJDBCNumericType(Class<?> type) {
            return type.equals(BigDecimal.class) || // NUMERIC
                type.equals(Byte.class) ||          // TINYINT
                type.equals(Short.class) ||         // SMALLINT
                type.equals(Integer.class) ||       // INTEGER
                type.equals(Float.class) ||         // FLOAT
                type.equals(Double.class);          // DOUBLE
        }

        private boolean isJDBCDateType(Class<?> type) {
            return type.equals(Date.class) ||       // DATE
                type.equals(Time.class) ||          // TIME
                type.equals(Timestamp.class) ||     // TIMESTAMP
                // Note :
                // INTERVAL SQL Type is not JDBC native and often DB specific.
                // We take here classic java representation.
                type.equals(Period.class) ||        // INTERVAL
                type.equals(Duration.class);        // INTERVAL
        }

        private String readBlob(Blob blob) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); InputStream inputStream = blob.getBinaryStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toString();
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
            finally {
                try {
                    blob.free(); // (JDBC 4.0+)
                } catch (SQLException e) {
                    LOGGER.warn("Failed to free Blob resources: {}", e.getMessage());
                }
            }
        }
    }
}
