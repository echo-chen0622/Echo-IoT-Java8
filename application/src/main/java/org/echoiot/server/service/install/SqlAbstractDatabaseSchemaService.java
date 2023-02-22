package org.echoiot.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库架构安装服务。这个服务是用来安装数据库架构的，包括创建表，创建索引，创建存储过程等等。
 * 抽象服务，需要子类来实现不同数据库的具体安装方法。
 * 不要给这个类加 @Service 注解，因为这个类是抽象类，不是具体的实现类！
 *
 * @author Echo
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

    @Contract(pure = true)
    protected SqlAbstractDatabaseSchemaService(String schemaSql, String schemaIdxSql) {
        this.schemaSql = schemaSql;
        this.schemaIdxSql = schemaIdxSql;
    }

    /**
     * 创建数据库架构
     *
     * @throws Exception
     */
    @Override
    public void createDatabaseSchema() throws Exception {
        // 创建数据库表（包括索引）
        this.createDatabaseSchema(true);
    }

    /**
     * 创建数据库架构
     *
     * @param createIndexes 是否创建索引
     *
     * @throws Exception
     */
    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("安装SQL 数据库架构部分: " + schemaSql);
        // 执行数据库安装脚本
        executeQueryFromFile(schemaSql);

        if (createIndexes) {
            // 创建数据库索引
            this.createDatabaseIndexes();
        }
    }

    /**
     * 创建数据库索引
     *
     * @throws Exception
     */
    @Override
    public void createDatabaseIndexes() throws Exception {
        if (schemaIdxSql != null) {
            log.info("安装数据库索引: " + schemaIdxSql);
            executeQueryFromFile(schemaIdxSql);
        }
    }

    /**
     * 从文件中执行SQL语句
     * 这里主要是为了执行创建表的脚本，不建议用做太多的事情
     *
     * @param schemaIdxSql SQL文件名
     *
     * @throws SQLException
     * @throws IOException
     */
    void executeQueryFromFile(String schemaIdxSql) throws SQLException, IOException {
        Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaIdxSql);
        // 读取文件内容
        String sql = Files.readString(schemaIdxFile);
        // 执行SQL语句。到这里，方法其实与 executeQuery(String query) 方法一样，只是不想打印大量 sql 日志，所以重复写了
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword); Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * 执行SQL语句
     * 这个类主要还是为了执行升级脚本的，不建议用做太多的事情
     *
     * @param query
     */
    protected void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword); Statement statement = conn.createStatement()) {
            statement.execute(query);
            log.info("已成功执行sql 语句: {}", query);
            //不懂这里为什么要睡 5 秒，不睡试试，目前（不配置 nosql 数据库的情况下）不睡没有问题
            //Thread.sleep(5000);
        } catch (Exception e) {
            log.error("执行 sql 失败: {} 报错信息: {}", query, e.getMessage());
            throw new RuntimeException("执行 sql 失败: " + query, e);
        }
    }

}
