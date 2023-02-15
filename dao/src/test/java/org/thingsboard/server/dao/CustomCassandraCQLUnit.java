package org.thingsboard.server.dao;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.BaseCassandraUnit;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.util.List;

public class CustomCassandraCQLUnit extends BaseCassandraUnit {
    protected List<CQLDataSet> dataSets;

    public CqlSession session;

    public CustomCassandraCQLUnit(List<CQLDataSet> dataSets) {
        this.dataSets = dataSets;
    }

    public CustomCassandraCQLUnit(List<CQLDataSet> dataSets, int readTimeoutMillis) {
        this.dataSets = dataSets;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public CustomCassandraCQLUnit(List<CQLDataSet> dataSets, String configurationFileName) {
        this(dataSets);
        this.configurationFileName = configurationFileName;
    }

    public CustomCassandraCQLUnit(List<CQLDataSet> dataSets, String configurationFileName, int readTimeoutMillis) {
        this(dataSets);
        this.configurationFileName = configurationFileName;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public CustomCassandraCQLUnit(List<CQLDataSet> dataSets, String configurationFileName, long startUpTimeoutMillis) {
        super(startUpTimeoutMillis);
        this.dataSets = dataSets;
        this.configurationFileName = configurationFileName;
    }

    public CustomCassandraCQLUnit(List<CQLDataSet> dataSets, String configurationFileName, long startUpTimeoutMillis, int readTimeoutMillis) {
        super(startUpTimeoutMillis);
        this.dataSets = dataSets;
        this.configurationFileName = configurationFileName;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    protected void load() {
        session = EmbeddedCassandraServerHelper.getSession();
        CQLDataLoader dataLoader = new CQLDataLoader(session);
        dataSets.forEach(dataLoader::load);
        session = dataLoader.getSession();
        System.setSecurityManager(null);
    }

    @Override
    protected void after() {
        super.after();
        try (CqlSession s = session) {
            session = null;
        }
        System.setSecurityManager(null);
    }

    // Getters for those who do not like to directly access fields

    public CqlSession getSession() {
        return session;
    }

}
