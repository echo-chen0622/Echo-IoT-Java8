package org.echoiot.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.stats.MessagesStats;
import org.echoiot.server.common.stats.StatsFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Data
public class TbSqlBlockingQueueWrapper<E> {
    private final CopyOnWriteArrayList<TbSqlBlockingQueue<E>> queues = new CopyOnWriteArrayList<>();
    @NotNull
    private final TbSqlBlockingQueueParams params;
    private ScheduledLogExecutorComponent logExecutor;
    @NotNull
    private final Function<E, Integer> hashCodeFunction;
    private final int maxThreads;
    @NotNull
    private final StatsFactory statsFactory;

    /**
     * Starts TbSqlBlockingQueues.
     *
     * @param  logExecutor  executor that will be printing logs and statistics
     * @param  saveFunction function to save entities in database
     * @param  batchUpdateComparator comparator to sort entities by primary key to avoid deadlocks in cluster mode
     *                               NOTE: you must use all of primary key parts in your comparator
     */
    public void init(@NotNull ScheduledLogExecutorComponent logExecutor, @NotNull Consumer<List<E>> saveFunction, Comparator<E> batchUpdateComparator) {
        for (int i = 0; i < maxThreads; i++) {
            MessagesStats stats = statsFactory.createMessagesStats(params.getStatsNamePrefix() + ".queue." + i);
            @NotNull TbSqlBlockingQueue<E> queue = new TbSqlBlockingQueue<>(params, stats);
            queues.add(queue);
            queue.init(logExecutor, saveFunction, batchUpdateComparator, i);
        }
    }

    public ListenableFuture<Void> add(@Nullable E element) {
        int queueIndex = element != null ? (hashCodeFunction.apply(element) & 0x7FFFFFFF) % maxThreads : 0;
        return queues.get(queueIndex).add(element);
    }

    public void destroy() {
        queues.forEach(TbSqlBlockingQueue::destroy);
    }
}
