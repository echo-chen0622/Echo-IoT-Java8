package org.echoiot.server.service.executors;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DbCallbackExecutorService extends AbstractListeningExecutor {

    @Value("${actors.rule.db_callback_thread_pool_size}")
    private int dbCallbackExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return dbCallbackExecutorThreadPoolSize;
    }

}
