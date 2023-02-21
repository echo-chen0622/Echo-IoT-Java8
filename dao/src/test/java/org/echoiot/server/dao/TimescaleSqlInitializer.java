package org.echoiot.server.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class TimescaleSqlInitializer {

    private static final List<String> sqlFiles = List.of(
            "sql/schema-timescale.sql",
            "sql/schema-entities.sql",
            "sql/schema-entities-idx.sql",
            "sql/schema-entities-idx-psql-addon.sql",
            "sql/system-data.sql",
            "sql/system-test-psql.sql");
    private static final String dropAllTablesSqlFile = "sql/timescale/drop-all-tables.sql";

    public static void initDb(@NotNull Connection conn) {
        cleanUpDb(conn);
        log.info("initialize Timescale DB...");
        try {
            for (@NotNull String sqlFile : sqlFiles) {
                @NotNull URL sqlFileUrl = Resources.getResource(sqlFile);
                @NotNull String sql = Resources.toString(sqlFileUrl, Charsets.UTF_8);
                conn.createStatement().execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to init the Timescale database. Reason: " + e.getMessage(), e);
        }
        log.info("Timescale DB is initialized!");
    }

    private static void cleanUpDb(@NotNull Connection conn) {
        log.info("clean up Timescale DB...");
        try {
            @NotNull URL dropAllTableSqlFileUrl = Resources.getResource(dropAllTablesSqlFile);
            @NotNull String dropAllTablesSql = Resources.toString(dropAllTableSqlFileUrl, Charsets.UTF_8);
            conn.createStatement().execute(dropAllTablesSql);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to clean up the Timescale database. Reason: " + e.getMessage(), e);
        }
    }
}
