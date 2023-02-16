package org.echoiot.server.dao.nosql;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.util.AbstractBufferedRateExecutor;
import org.echoiot.server.dao.util.AsyncTaskContext;
import org.echoiot.server.dao.util.NoSqlAnyDao;

import javax.annotation.PreDestroy;

/**
 * Created by Echo on 24.10.18.
 */
@Component
@Slf4j
@NoSqlAnyDao
public class CassandraBufferedRateWriteExecutor extends AbstractBufferedRateExecutor<CassandraStatementTask, TbResultSetFuture, TbResultSet> {

    static final String BUFFER_NAME = "Write";

    public CassandraBufferedRateWriteExecutor(
            @Value("${cassandra.query.buffer_size}") int queueLimit,
            @Value("${cassandra.query.concurrent_limit}") int concurrencyLimit,
            @Value("${cassandra.query.permit_max_wait_time}") long maxWaitTime,
            @Value("${cassandra.query.dispatcher_threads:2}") int dispatcherThreads,
            @Value("${cassandra.query.callback_threads:4}") int callbackThreads,
            @Value("${cassandra.query.poll_ms:50}") long pollMs,
            @Value("${cassandra.query.tenant_rate_limits.print_tenant_names}") boolean printTenantNames,
            @Value("${cassandra.query.print_queries_freq:0}") int printQueriesFreq,
            @Autowired StatsFactory statsFactory,
            @Autowired EntityService entityService,
            @Autowired TbTenantProfileCache tenantProfileCache) {
        super(queueLimit, concurrencyLimit, maxWaitTime, dispatcherThreads, callbackThreads, pollMs, printQueriesFreq, statsFactory,
                entityService, tenantProfileCache, printTenantNames);
    }

    @Scheduled(fixedDelayString = "${cassandra.query.rate_limit_print_interval_ms}")
    @Override
    public void printStats() {
        super.printStats();
    }

    @PreDestroy
    public void stop() {
        super.stop();
    }

    @Override
    public String getBufferName() {
        return BUFFER_NAME;
    }

    @Override
    protected SettableFuture<TbResultSet> create() {
        return SettableFuture.create();
    }

    @Override
    protected TbResultSetFuture wrap(CassandraStatementTask task, SettableFuture<TbResultSet> future) {
        return new TbResultSetFuture(future);
    }

    @Override
    protected ListenableFuture<TbResultSet> execute(AsyncTaskContext<CassandraStatementTask, TbResultSet> taskCtx) {
        CassandraStatementTask task = taskCtx.getTask();
        return task.executeAsync(
                statement ->
                        this.submit(new CassandraStatementTask(task.getTenantId(), task.getSession(), statement))
        );
    }

}