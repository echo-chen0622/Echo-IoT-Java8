package org.echoiot.server.service.install.migrate;

import com.datastax.oss.driver.api.core.cql.Row;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CassandraToSqlEventTsColumn extends CassandraToSqlColumn {

    CassandraToSqlEventTsColumn() {
        super("id", "ts", CassandraToSqlColumnType.BIGINT, null, false);
    }

    @Override
    public String getColumnValue(Row row) {
        @Nullable UUID id = row.getUuid(getIndex());
        long ts = getTs(id);
        return ts + "";
    }

    private long getTs(UUID uuid) {
        return (uuid.timestamp() - ModelConstants.EPOCH_DIFF) / 10000;
    }
}
