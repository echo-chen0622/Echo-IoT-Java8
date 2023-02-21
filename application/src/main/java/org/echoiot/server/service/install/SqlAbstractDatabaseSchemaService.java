package org.echoiot.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库架构安装服务。这个服务是用来安装数据库架构的，包括创建表，创建索引，创建存储过程等等。
 * 抽象服务，需要子类来实现不同数据库的具体安装方法。
 * 不要给这个类加 @Service 注解，因为这个类是抽象类，不是具体的实现类！
 */
@Slf4j
public abstract class SqlAbstractDatabaseSchemaService implements DatabaseSchemaService {

    protected static final String SQL_DIR = "sql";

    /**
     * 数据库连接
     */
    @Value("${spring.datasource.url}")
    protected String dbUrl;

    /**
     * 数据库用户名
     */
    @Value("${spring.datasource.username}")
    protected String dbUserName;

    /**
     * 数据库密码
     */
    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Resource
    protected InstallScripts installScripts;

    private final String schemaSql;
    private final String schemaIdxSql;

    protected SqlAbstractDatabaseSchemaService(String schemaSql, String schemaIdxSql) {
        this.schemaSql = schemaSql;
        this.schemaIdxSql = schemaIdxSql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("安装SQL 数据库架构部分: " + schemaSql);
        executeQueryFromFile(schemaSql);

        if (createIndexes) {
            this.createDatabaseIndexes();
        }
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
        if (schemaIdxSql != null) {
            log.info("Installing SQL DataBase schema indexes part: " + schemaIdxSql);
            executeQueryFromFile(schemaIdxSql);
        }
    }

    void executeQueryFromFile(String schemaIdxSql) throws SQLException, IOException {
        @NotNull Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaIdxSql);
        String sql = Files.readString(schemaIdxFile);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial echoiot database schema
        }
    }

    protected void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute echoiot database upgrade script
            log.info("Successfully executed query: {}", query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.error("Failed to execute query: {} due to: {}", query, e.getMessage());
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
    }

}
