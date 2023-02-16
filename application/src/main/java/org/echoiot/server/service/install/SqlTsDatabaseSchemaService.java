package org.echoiot.server.service.install;

import org.echoiot.server.dao.util.SqlTsDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@SqlTsDao
@Profile("install")
public class SqlTsDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements TsDatabaseSchemaService {

    @Value("${sql.postgres.ts_key_value_partitioning:MONTHS}")
    private String partitionType;

    public SqlTsDatabaseSchemaService() {
        super("schema-ts-psql.sql", null);
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        super.createDatabaseSchema();
        executeQuery("CREATE TABLE IF NOT EXISTS ts_kv_indefinite PARTITION OF ts_kv DEFAULT;");
    }
}
