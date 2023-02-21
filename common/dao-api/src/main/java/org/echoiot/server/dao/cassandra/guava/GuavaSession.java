package org.echoiot.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.session.Session;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.cql.DefaultPrepareRequest;
import com.datastax.oss.driver.internal.core.cql.SinglePageResultSet;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;

public interface GuavaSession extends Session, SyncCqlSession {

    GenericType<ListenableFuture<AsyncResultSet>> ASYNC =
            new GenericType<ListenableFuture<AsyncResultSet>>() {};

    GenericType<ListenableFuture<PreparedStatement>> ASYNC_PREPARED =
            new GenericType<ListenableFuture<PreparedStatement>>() {};

    @NonNull
    default ResultSet execute(@NonNull Statement<?> statement) {
        AsyncResultSet firstPage = getSafe(this.executeAsync(statement));
        if (firstPage.hasMorePages()) {
            return new GuavaMultiPageResultSet(this, statement, firstPage);
        } else {
            return new SinglePageResultSet(firstPage);
        }
    }

    @Nullable
    default ListenableFuture<AsyncResultSet> executeAsync(@NotNull Statement<?> statement) {
        return this.execute(statement, ASYNC);
    }

    default ListenableFuture<AsyncResultSet> executeAsync(@NotNull String statement) {
        return this.executeAsync(SimpleStatement.newInstance(statement));
    }

    @Nullable
    default ListenableFuture<PreparedStatement> prepareAsync(SimpleStatement statement) {
        return this.execute(new DefaultPrepareRequest(statement), ASYNC_PREPARED);
    }

    default ListenableFuture<PreparedStatement> prepareAsync(@NotNull String statement) {
        return this.prepareAsync(SimpleStatement.newInstance(statement));
    }

    static AsyncResultSet getSafe(@NotNull ListenableFuture<AsyncResultSet> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
