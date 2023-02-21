package org.echoiot.server.dao.cassandra;

import org.echoiot.server.dao.util.NoSqlAnyDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("CassandraCluster")
@NoSqlAnyDao
public class CassandraCluster extends AbstractCassandraCluster {

    @Value("${cassandra.keyspace_name}")
    private String keyspaceName;

    @PostConstruct
    public void init() {
        super.init(keyspaceName);
    }

}
