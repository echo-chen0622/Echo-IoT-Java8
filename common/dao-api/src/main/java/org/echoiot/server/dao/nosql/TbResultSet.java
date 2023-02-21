package org.echoiot.server.dao.nosql;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class TbResultSet implements AsyncResultSet {

    private final Statement originalStatement;
    private final AsyncResultSet delegate;
    private final Function<Statement, TbResultSetFuture> executeAsyncFunction;

    public TbResultSet(Statement originalStatement, AsyncResultSet delegate,
                       Function<Statement, TbResultSetFuture> executeAsyncFunction) {
        this.originalStatement = originalStatement;
        this.delegate = delegate;
        this.executeAsyncFunction = executeAsyncFunction;
    }

    @NonNull
    @Override
    public ColumnDefinitions getColumnDefinitions() {
        return delegate.getColumnDefinitions();
    }

    @NonNull
    @Override
    public ExecutionInfo getExecutionInfo() {
        return delegate.getExecutionInfo();
    }

    @Override
    public int remaining() {
        return delegate.remaining();
    }

    @NonNull
    @Override
    public Iterable<Row> currentPage() {
        return delegate.currentPage();
    }

    @Override
    public boolean hasMorePages() {
        return delegate.hasMorePages();
    }

    @NonNull
    @Override
    public CompletionStage<AsyncResultSet> fetchNextPage() throws IllegalStateException {
        return delegate.fetchNextPage();
    }

    @Override
    public boolean wasApplied() {
        return delegate.wasApplied();
    }

    @NotNull
    public ListenableFuture<List<Row>> allRows(Executor executor) {
        @NotNull List<Row> allRows = new ArrayList<>();
        @NotNull SettableFuture<List<Row>> resultFuture = SettableFuture.create();
        this.processRows(originalStatement, delegate, allRows, resultFuture, executor);
        return resultFuture;
    }

    private void processRows(@NotNull Statement statement,
                             @NotNull AsyncResultSet resultSet,
                             @NotNull List<Row> allRows,
                             @NotNull SettableFuture<List<Row>> resultFuture,
                             @org.jetbrains.annotations.Nullable Executor executor) {
        allRows.addAll(loadRows(resultSet));
        if (resultSet.hasMorePages()) {
            @org.jetbrains.annotations.Nullable ByteBuffer nextPagingState = resultSet.getExecutionInfo().getPagingState();
            @NotNull Statement<?> nextStatement = statement.setPagingState(nextPagingState);
            TbResultSetFuture resultSetFuture = executeAsyncFunction.apply(nextStatement);
            Futures.addCallback(resultSetFuture,
                    new FutureCallback<TbResultSet>() {
                        @Override
                        public void onSuccess(@Nullable TbResultSet result) {
                            processRows(nextStatement, result,
                                    allRows, resultFuture, executor);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            resultFuture.setException(t);
                        }
                    }, executor != null ? executor : MoreExecutors.directExecutor()
            );
        } else {
            resultFuture.set(allRows);
        }
    }

    @NotNull
    List<Row> loadRows(@NotNull AsyncResultSet resultSet) {
        return Lists.newArrayList(resultSet.currentPage());
    }

}
