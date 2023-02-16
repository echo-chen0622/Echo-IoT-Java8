package org.echoiot.server.transport.lwm2m.server.common;

import org.echoiot.common.util.EchoiotExecutors;

import java.util.concurrent.ExecutorService;

public abstract class LwM2MExecutorAwareService {

    protected ExecutorService executor;

    protected abstract int getExecutorSize();

    protected abstract String getExecutorName();

    protected void init() {
        this.executor = EchoiotExecutors.newWorkStealingPool(getExecutorSize(), getExecutorName());
    }

    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
