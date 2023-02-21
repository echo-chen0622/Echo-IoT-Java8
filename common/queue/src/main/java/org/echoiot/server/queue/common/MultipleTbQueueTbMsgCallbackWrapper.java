package org.echoiot.server.queue.common;

import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsgMetadata;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class MultipleTbQueueTbMsgCallbackWrapper implements TbQueueCallback {

    @NotNull
    private final AtomicInteger tbQueueCallbackCount;
    private final TbMsgCallback tbMsgCallback;

    public MultipleTbQueueTbMsgCallbackWrapper(int tbQueueCallbackCount, TbMsgCallback tbMsgCallback) {
        this.tbQueueCallbackCount = new AtomicInteger(tbQueueCallbackCount);
        this.tbMsgCallback = tbMsgCallback;
    }

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        if (tbQueueCallbackCount.decrementAndGet() <= 0) {
            tbMsgCallback.onSuccess();
        }
    }

    @Override
    public void onFailure(@NotNull Throwable t) {
        tbMsgCallback.onFailure(new RuleEngineException(t.getMessage()));
    }
}
