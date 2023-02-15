package org.thingsboard.server.dao.nosql;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 21.02.17.
 */
public abstract class CassandraAbstractAsyncDao extends CassandraAbstractDao {

    protected ExecutorService readResultsProcessingExecutor;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("cassandra-callback"));
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    protected <T> ListenableFuture<T> getFuture(TbResultSetFuture future, java.util.function.Function<TbResultSet, T> transformer) {
        return Futures.transform(future, new Function<TbResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable TbResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    protected <T> ListenableFuture<T> getFutureAsync(TbResultSetFuture future, com.google.common.util.concurrent.AsyncFunction<TbResultSet, T> transformer) {
        return Futures.transformAsync(future, new AsyncFunction<TbResultSet, T>() {
            @Nullable
            @Override
            public ListenableFuture<T> apply(@Nullable TbResultSet input) {
                try {
                    return transformer.apply(input);
                } catch (Exception e) {
                    return Futures.immediateFailedFuture(e);
                }
            }
        }, readResultsProcessingExecutor);
    }

}
