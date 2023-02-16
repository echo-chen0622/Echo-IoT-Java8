package org.echoiot.server.service.executors;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GrpcCallbackExecutorService extends AbstractListeningExecutor {

    @Value("${edges.grpc_callback_thread_pool_size}")
    private int grpcCallbackExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return grpcCallbackExecutorThreadPoolSize;
    }

}
