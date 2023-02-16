package org.echoiot.server.service.install;

import org.echoiot.server.dao.util.NoSqlTsDao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@NoSqlTsDao
@Profile("install")
public class CassandraTsDatabaseSchemaService extends CassandraAbstractDatabaseSchemaService
        implements TsDatabaseSchemaService {
    public CassandraTsDatabaseSchemaService() {
        super("schema-ts.cql");
    }
}
