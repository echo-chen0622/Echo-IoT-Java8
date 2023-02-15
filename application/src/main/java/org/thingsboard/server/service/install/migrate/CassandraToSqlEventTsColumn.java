package org.thingsboard.server.service.install.migrate;

import com.datastax.oss.driver.api.core.cql.Row;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;

public class CassandraToSqlEventTsColumn extends CassandraToSqlColumn {

    CassandraToSqlEventTsColumn() {
        super("id", "ts", CassandraToSqlColumnType.BIGINT, null, false);
    }

    @Override
    public String getColumnValue(Row row) {
        UUID id = row.getUuid(getIndex());
        long ts = getTs(id);
        return ts + "";
    }

    private long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
