package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public interface TbSqlQueue<E> {

    void init(ScheduledLogExecutorComponent logExecutor, Consumer<List<E>> saveFunction, Comparator<E> batchUpdateComparator, int queueIndex);

    void destroy();

    ListenableFuture<Void> add(E element);
}
