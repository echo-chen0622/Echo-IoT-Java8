package org.echoiot.server.queue.common;

import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsgMetadata;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class MultipleTbQueueCallbackWrapper implements TbQueueCallback {

    @NotNull
    private final AtomicInteger tbQueueCallbackCount;
    private final TbQueueCallback callback;

    public MultipleTbQueueCallbackWrapper(int tbQueueCallbackCount, TbQueueCallback callback) {
        this.tbQueueCallbackCount = new AtomicInteger(tbQueueCallbackCount);
        this.callback = callback;
    }

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        if (tbQueueCallbackCount.decrementAndGet() <= 0) {
            callback.onSuccess(metadata);
        }
    }

    @Override
    public void onFailure(@NotNull Throwable t) {
        callback.onFailure(new RuleEngineException(t.getMessage()));
    }
}
