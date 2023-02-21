package org.echoiot.server.service.install.migrate;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.UUIDConverter;
import org.echoiot.server.dao.cassandra.guava.GuavaSession;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Data
@Slf4j
public class CassandraToSqlTable {

    private static final int DEFAULT_BATCH_SIZE = 10000;

    private String cassandraCf;
    private String sqlTableName;

    private List<CassandraToSqlColumn> columns;

    private int batchSize = DEFAULT_BATCH_SIZE;

    private PreparedStatement sqlInsertStatement;

    public CassandraToSqlTable(String tableName, CassandraToSqlColumn... columns) {
        this(tableName, tableName, DEFAULT_BATCH_SIZE, columns);
    }

    public CassandraToSqlTable(String tableName, String sqlTableName, CassandraToSqlColumn... columns) {
        this(tableName, sqlTableName, DEFAULT_BATCH_SIZE, columns);
    }

    public CassandraToSqlTable(String tableName, int batchSize, CassandraToSqlColumn... columns) {
        this(tableName, tableName, batchSize, columns);
    }

    public CassandraToSqlTable(String cassandraCf, String sqlTableName, int batchSize, @NotNull CassandraToSqlColumn... columns) {
        this.cassandraCf = cassandraCf;
        this.sqlTableName = sqlTableName;
        this.batchSize = batchSize;
        this.columns = Arrays.asList(columns);
        for (int i=0;i<columns.length;i++) {
            this.columns.get(i).setIndex(i);
            this.columns.get(i).setSqlIndex(i+1);
        }
    }

    public void migrateToSql(@NotNull GuavaSession session, @NotNull Connection conn) throws SQLException {
        log.info("[{}] Migrating data from cassandra '{}' Column Family to '{}' SQL table...", this.sqlTableName, this.cassandraCf, this.sqlTableName);
        DatabaseMetaData metadata = conn.getMetaData();
        java.sql.ResultSet resultSet = metadata.getColumns(null, null, this.sqlTableName, null);
        while (resultSet.next()) {
            String name = resultSet.getString("COLUMN_NAME");
            int sqlType = resultSet.getInt("DATA_TYPE");
            int size = resultSet.getInt("COLUMN_SIZE");
            @NotNull CassandraToSqlColumn column = this.getColumn(name);
            column.setSize(size);
            column.setSqlType(sqlType);
        }
        this.sqlInsertStatement = createSqlInsertStatement(conn);
        @NotNull Statement cassandraSelectStatement = createCassandraSelectStatement();
        cassandraSelectStatement.setPageSize(100);
        @NotNull ResultSet rs = session.execute(cassandraSelectStatement);
        @NotNull Iterator<Row> iter = rs.iterator();
        int rowCounter = 0;
        List<CassandraToSqlColumnData[]> batchData;
        boolean hasNext;
        do {
            batchData = this.extractBatchData(iter);
            hasNext = batchData.size() == this.batchSize;
            this.batchInsert(batchData, conn);
            rowCounter += batchData.size();
            log.info("[{}] {} records migrated so far...", this.sqlTableName, rowCounter);
        } while (hasNext);
        this.sqlInsertStatement.close();
        log.info("[{}] {} total records migrated.", this.sqlTableName, rowCounter);
        log.info("[{}] Finished migration data from cassandra '{}' Column Family to '{}' SQL table.",
                this.sqlTableName, this.cassandraCf, this.sqlTableName);
    }

    @NotNull
    private List<CassandraToSqlColumnData[]> extractBatchData(@NotNull Iterator<Row> iter) {
        @NotNull List<CassandraToSqlColumnData[]> batchData = new ArrayList<>();
        while (iter.hasNext() && batchData.size() < this.batchSize) {
            Row row = iter.next();
            if (row != null) {
                CassandraToSqlColumnData[] data = this.extractRowData(row);
                batchData.add(data);
            }
        }
        return batchData;
    }

    private CassandraToSqlColumnData[] extractRowData(@NotNull Row row) {
        @NotNull CassandraToSqlColumnData[] data = new CassandraToSqlColumnData[this.columns.size()];
        for (@NotNull CassandraToSqlColumn column: this.columns) {
            String value = column.getColumnValue(row);
            data[column.getIndex()] = new CassandraToSqlColumnData(value);
        }
        return this.validateColumnData(data);
    }

    protected CassandraToSqlColumnData[] validateColumnData(@NotNull CassandraToSqlColumnData[] data) {
        for (int i=0;i<data.length;i++) {
            CassandraToSqlColumn column = this.columns.get(i);
            if (column.getType() == CassandraToSqlColumnType.STRING) {
                CassandraToSqlColumnData columnData = data[i];
                String value = columnData.getValue();
                if (value != null && value.length() > column.getSize()) {
                    log.warn("[{}] Value size [{}] exceeds maximum size [{}] of column [{}] and will be truncated!",
                            this.sqlTableName,
                            value.length(), column.getSize(), column.getSqlColumnName());
                    log.warn("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
                    value = value.substring(0, column.getSize());
                    columnData.setOriginalValue(value);
                    columnData.setValue(value);
                }
            }
        }
        return data;
    }

    protected void batchInsert(@NotNull List<CassandraToSqlColumnData[]> batchData, @NotNull Connection conn) throws SQLException {
        boolean retry = false;
        for (@NotNull CassandraToSqlColumnData[] data : batchData) {
            for (@NotNull CassandraToSqlColumn column: this.columns) {
                column.setColumnValue(this.sqlInsertStatement, data[column.getIndex()].getValue());
            }
            try {
                this.sqlInsertStatement.executeUpdate();
            } catch (SQLException e) {
                if (this.handleInsertException(batchData, data, conn, e)) {
                    retry = true;
                    break;
                } else {
                    throw e;
                }
            }
        }
        if (retry) {
            this.batchInsert(batchData, conn);
        } else {
            conn.commit();
        }
    }

    private boolean handleInsertException(List<CassandraToSqlColumnData[]> batchData,
                                          @NotNull CassandraToSqlColumnData[] data,
                                          @NotNull Connection conn, @NotNull SQLException ex) throws SQLException {
        conn.commit();
        @Nullable String constraint = extractConstraintName(ex).orElse(null);
        if (constraint != null) {
            if (this.onConstraintViolation(batchData, data, constraint)) {
                return true;
            } else {
                log.error("[{}] Unhandled constraint violation [{}] during insert!", this.sqlTableName, constraint);
                log.error("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
            }
        } else {
            log.error("[{}] Unhandled exception during insert!", this.sqlTableName);
            log.error("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
        }
        return false;
    }

    @NotNull
    private String dataToString(@NotNull CassandraToSqlColumnData[] data) {
        @NotNull StringBuffer stringData = new StringBuffer("{\n");
        for (int i=0;i<data.length;i++) {
            String columnName = this.columns.get(i).getSqlColumnName();
            String value = data[i].getLogValue();
            stringData.append("\"").append(columnName).append("\": ").append("[").append(value).append("]\n");
        }
        stringData.append("}");
        return stringData.toString();
    }

    protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                            CassandraToSqlColumnData[] data, String constraint) {
        return false;
    }

    protected void handleUniqueNameViolation(@NotNull CassandraToSqlColumnData[] data, String entityType) {
        @NotNull CassandraToSqlColumn nameColumn = this.getColumn("name");
        @NotNull CassandraToSqlColumn searchTextColumn = this.getColumn("search_text");
        CassandraToSqlColumnData nameColumnData = data[nameColumn.getIndex()];
        CassandraToSqlColumnData searchTextColumnData = data[searchTextColumn.getIndex()];
        String prevName = nameColumnData.getValue();
        String newName = nameColumnData.getNextConstraintStringValue(nameColumn);
        nameColumnData.setValue(newName);
        searchTextColumnData.setValue(searchTextColumnData.getNextConstraintStringValue(searchTextColumn));
        String id = UUIDConverter.fromString(this.getColumnData(data, "id").getValue()).toString();
        log.warn("Found {} with duplicate name [id:[{}]]. Attempting to rename {} from '{}' to '{}'...", entityType, id, entityType, prevName, newName);
    }

    protected void handleUniqueEmailViolation(@NotNull CassandraToSqlColumnData[] data) {
        @NotNull CassandraToSqlColumn emailColumn = this.getColumn("email");
        @NotNull CassandraToSqlColumn searchTextColumn = this.getColumn("search_text");
        CassandraToSqlColumnData emailColumnData = data[emailColumn.getIndex()];
        CassandraToSqlColumnData searchTextColumnData = data[searchTextColumn.getIndex()];
        String prevEmail = emailColumnData.getValue();
        String newEmail = emailColumnData.getNextConstraintEmailValue(emailColumn);
        emailColumnData.setValue(newEmail);
        searchTextColumnData.setValue(searchTextColumnData.getNextConstraintEmailValue(searchTextColumn));
        String id = UUIDConverter.fromString(this.getColumnData(data, "id").getValue()).toString();
        log.warn("Found user with duplicate email [id:[{}]]. Attempting to rename email from '{}' to '{}'...", id, prevEmail, newEmail);
    }

    protected void ignoreRecord(@NotNull List<CassandraToSqlColumnData[]> batchData, @NotNull CassandraToSqlColumnData[] data) {
        log.warn("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
        int index = batchData.indexOf(data);
        if (index > 0) {
            batchData.remove(index);
        }
    }

    @NotNull
    protected CassandraToSqlColumn getColumn(String sqlColumnName) {
        return this.columns.stream().filter(col -> col.getSqlColumnName().equals(sqlColumnName)).findFirst().get();
    }

    protected CassandraToSqlColumnData getColumnData(@NotNull CassandraToSqlColumnData[] data, String sqlColumnName) {
        @NotNull CassandraToSqlColumn column = this.getColumn(sqlColumnName);
        return data[column.getIndex()];
    }

    @NotNull
    private Optional<String> extractConstraintName(@NotNull SQLException ex) {
        final String sqlState = JdbcExceptionHelper.extractSqlState( ex );
        if (sqlState != null) {
            @NotNull String sqlStateClassCode = JdbcExceptionHelper.determineSqlStateClassCode(sqlState);
            if ( sqlStateClassCode != null ) {
                if (Arrays.asList(
                        "23",	// "integrity constraint violation"
                        "27",	// "triggered data change violation"
                        "44"	// "with check option violation"
                ).contains(sqlStateClassCode)) {
                    if (ex instanceof PSQLException) {
                        return Optional.of(((PSQLException)ex).getServerErrorMessage().getConstraint());
                    }
                }
            }
        }
        return Optional.empty();
    }

    @NotNull
    protected Statement createCassandraSelectStatement() {
        @NotNull StringBuilder selectStatementBuilder = new StringBuilder();
        selectStatementBuilder.append("SELECT ");
        for (@NotNull CassandraToSqlColumn column : columns) {
            selectStatementBuilder.append(column.getCassandraColumnName()).append(",");
        }
        selectStatementBuilder.deleteCharAt(selectStatementBuilder.length() - 1);
        selectStatementBuilder.append(" FROM ").append(cassandraCf);
        return SimpleStatement.newInstance(selectStatementBuilder.toString());
    }

    private PreparedStatement createSqlInsertStatement(@NotNull Connection conn) throws SQLException {
        @NotNull StringBuilder insertStatementBuilder = new StringBuilder();
        insertStatementBuilder.append("INSERT INTO ").append(this.sqlTableName).append(" (");
        for (@NotNull CassandraToSqlColumn column : columns) {
            insertStatementBuilder.append(column.getSqlColumnName()).append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (@NotNull CassandraToSqlColumn column : columns) {
            if (column.getType() == CassandraToSqlColumnType.JSON) {
                insertStatementBuilder.append("cast(? AS json)");
            } else {
                insertStatementBuilder.append("?");
            }
            insertStatementBuilder.append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");
        return conn.prepareStatement(insertStatementBuilder.toString());
    }

}
