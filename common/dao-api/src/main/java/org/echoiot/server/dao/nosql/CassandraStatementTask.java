package org.echoiot.server.dao.nosql;

import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.cassandra.guava.GuavaSession;
import org.echoiot.server.dao.util.AsyncTask;

import java.util.function.Function;

/**
 * Created by Echo on 24.10.18.
 */
@Data
public class CassandraStatementTask implements AsyncTask {

    private final TenantId tenantId;
    private final GuavaSession session;
    private final Statement statement;

    public ListenableFuture<TbResultSet> executeAsync(Function<Statement, TbResultSetFuture> executeAsyncFunction) {
        return Futures.transform(session.executeAsync(statement),
                result -> new TbResultSet(statement, result, executeAsyncFunction),
                MoreExecutors.directExecutor()
        );
    }

}