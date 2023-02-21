package org.echoiot.server.service.install.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.echoiot.server.service.install.DatabaseHelper.CSV_DUMP_FORMAT;

/**
 * Created by igor on 2/27/18.
 */
@Slf4j
public class SqlDbHelper {

    @Nullable
    public static Path dumpTableIfExists(@NotNull Connection conn, String tableName,
                                         @NotNull String[] columns, String[] defaultValues, String dumpPrefix) throws Exception {
        return dumpTableIfExists(conn, tableName, columns, defaultValues, dumpPrefix, false);
    }

    @Nullable
    public static Path dumpTableIfExists(@NotNull Connection conn, String tableName,
                                         @NotNull String[] columns, String[] defaultValues, String dumpPrefix, boolean printHeader) throws Exception {

        if (tableExists(conn, tableName)) {
            Path dumpFile = Files.createTempFile(dumpPrefix, null);
            Files.deleteIfExists(dumpFile);
            @NotNull CSVFormat csvFormat = CSV_DUMP_FORMAT;
            if (printHeader) {
                csvFormat = csvFormat.withHeader(columns);
            }
            try (@NotNull CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(dumpFile), csvFormat)) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + tableName)) {
                    try (ResultSet tableRes = stmt.executeQuery()) {
                        ResultSetMetaData resMetaData = tableRes.getMetaData();
                        @NotNull Map<String, Integer> columnIndexMap = new HashMap<>();
                        for (int i = 1; i <= resMetaData.getColumnCount(); i++) {
                            String columnName = resMetaData.getColumnName(i);
                            columnIndexMap.put(columnName.toUpperCase(), i);
                        }
                        while(tableRes.next()) {
                            dumpRow(tableRes, columnIndexMap, columns, defaultValues, csvPrinter);
                        }
                    }
                }
            }
            return dumpFile;
        } else {
            return null;
        }
    }

    private static boolean tableExists(@NotNull Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("select * from " + tableName + " where 1=0");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void loadTable(@NotNull Connection conn, String tableName, @NotNull String[] columns, @NotNull Path sourceFile) throws Exception {
        loadTable(conn, tableName, columns, sourceFile, false);
    }

    public static void loadTable(@NotNull Connection conn, String tableName, @NotNull String[] columns, @NotNull Path sourceFile, boolean parseHeader) throws Exception {
        @NotNull CSVFormat csvFormat = CSV_DUMP_FORMAT;
        if (parseHeader) {
            csvFormat = csvFormat.withFirstRecordAsHeader();
        } else {
            csvFormat = CSV_DUMP_FORMAT.withHeader(columns);
        }
        try (PreparedStatement prepared = conn.prepareStatement(createInsertStatement(tableName, columns))) {
            try (@NotNull CSVParser csvParser = new CSVParser(Files.newBufferedReader(sourceFile), csvFormat)) {
                csvParser.forEach(record -> {
                    try {
                        for (int i = 0; i < columns.length; i++) {
                            setColumnValue(i, columns[i], record, prepared);
                        }
                        prepared.execute();
                    } catch (SQLException e) {
                        log.error("Unable to load table record!", e);
                    }
                });
            }
        }
    }

    private static void dumpRow(@NotNull ResultSet res, @NotNull Map<String, Integer> columnIndexMap, @NotNull String[] columns,
                                @Nullable String[] defaultValues, @NotNull CSVPrinter csvPrinter) throws Exception {
        @NotNull List<String> record = new ArrayList<>();
        for (int i=0;i<columns.length;i++) {
            String column = columns[i];
            String defaultValue;
            if (defaultValues != null && i < defaultValues.length) {
                defaultValue = defaultValues[i];
            } else {
                defaultValue = "";
            }
            record.add(getColumnValue(column, defaultValue, columnIndexMap, res));
        }
        csvPrinter.printRecord(record);
    }

    @Nullable
    private static String getColumnValue(@NotNull String column, String defaultValue, @NotNull Map<String, Integer> columnIndexMap, @NotNull ResultSet res) {
        int index = columnIndexMap.containsKey(column.toUpperCase()) ? columnIndexMap.get(column.toUpperCase()) : -1;
        if (index > -1) {
            String str;
            try {
                Object obj = res.getObject(index);
                if (obj == null) {
                    return null;
                } else {
                    str = obj.toString();
                }
            } catch (Exception e) {
                str = "";
            }
            return str;
        } else {
            return defaultValue;
        }
    }

    private static void setColumnValue(int index, String column,
                                       @NotNull CSVRecord record, @NotNull PreparedStatement preparedStatement) throws SQLException {
        String value = record.get(column);
        int type = preparedStatement.getParameterMetaData().getParameterType(index + 1);
        preparedStatement.setObject(index + 1, value, type);
    }

    @NotNull
    private static String createInsertStatement(String tableName, @NotNull String[] columns) {
        @NotNull StringBuilder insertStatementBuilder = new StringBuilder();
        insertStatementBuilder.append("INSERT INTO ").append(tableName).append(" (");
        for (String column : columns) {
            insertStatementBuilder.append(column).append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (String column : columns) {
            insertStatementBuilder.append("?").append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");
        return insertStatementBuilder.toString();
    }

}
