package org.echoiot.server.service.install;

import org.echoiot.server.dao.util.NoSqlTsLatestDao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@NoSqlTsLatestDao
@Profile("install")
public class CassandraTsLatestDatabaseSchemaService extends CassandraAbstractDatabaseSchemaService
        implements TsLatestDatabaseSchemaService {
    public CassandraTsLatestDatabaseSchemaService() {
        super("schema-ts-latest.cql");
    }
}
