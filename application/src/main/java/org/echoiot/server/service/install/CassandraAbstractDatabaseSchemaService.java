package org.echoiot.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.cassandra.CassandraInstallCluster;
import org.echoiot.server.service.install.cql.CQLStatementsParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public abstract class CassandraAbstractDatabaseSchemaService implements DatabaseSchemaService {

    private static final String CASSANDRA_DIR = "cassandra";
    private static final String CASSANDRA_STANDARD_KEYSPACE = "echoiot";

    @Resource(name = "CassandraInstallCluster")
    private CassandraInstallCluster cluster;

    @Resource
    private InstallScripts installScripts;

    @Value("${cassandra.keyspace_name}")
    private String keyspaceName;

    private final String schemaCql;

    @Contract(pure = true)
    protected CassandraAbstractDatabaseSchemaService(String schemaCql) {
        this.schemaCql = schemaCql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("安装 Cassandra 数据库 架构部分: {}", schemaCql);
        // 获取 sql 文件路径
        Path schemaFile = Paths.get(installScripts.getDataDir(), CASSANDRA_DIR, schemaCql);
        // 装载并执行文件
        loadCql(schemaFile);
    }

    /**
     * Cassandra 数据库没有索引
     *
     * @throws Exception
     */
    @Override
    public void createDatabaseIndexes() throws Exception {
    }

    /**
     * 装载并执行文件
     *
     * @param cql
     *
     * @throws Exception
     */
    private void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> cluster.getSession().execute(getCassandraKeyspaceName(statement)));
    }

    /**
     * 替换 keyspace
     *
     * @param statement
     */
    @Contract(pure = true)
    private @NotNull String getCassandraKeyspaceName(String statement) {
        return statement.replaceFirst(CASSANDRA_STANDARD_KEYSPACE, keyspaceName);
    }
}
