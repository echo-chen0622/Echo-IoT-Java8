package org.echoiot.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.cassandra.CassandraCluster;
import org.echoiot.server.dao.cassandra.CassandraInstallCluster;
import org.echoiot.server.service.install.cql.CQLStatementsParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public abstract class AbstractCassandraDatabaseUpgradeService {
    @Resource
    protected CassandraCluster cluster;

    @Resource(name = "CassandraInstallCluster")
    private CassandraInstallCluster installCluster;

    protected void loadCql(@NotNull Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> {
            installCluster.getSession().execute(statement);
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }
        });
        Thread.sleep(5000);
    }
}
