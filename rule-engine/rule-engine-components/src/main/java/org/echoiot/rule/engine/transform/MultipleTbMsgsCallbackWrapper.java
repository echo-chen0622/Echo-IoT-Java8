package org.echoiot.rule.engine.transform;

import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class MultipleTbMsgsCallbackWrapper implements TbMsgCallbackWrapper {

    @NotNull
    private final AtomicInteger tbMsgsCallbackCount;
    private final TbMsgCallback callback;

    public MultipleTbMsgsCallbackWrapper(int tbMsgsCallbackCount, TbMsgCallback callback) {
        this.tbMsgsCallbackCount = new AtomicInteger(tbMsgsCallbackCount);
        this.callback = callback;
    }

    @Override
    public void onSuccess() {
        if (tbMsgsCallbackCount.decrementAndGet() <= 0) {
            callback.onSuccess();
        }
    }

    @Override
    public void onFailure(@NotNull Throwable t) {
        callback.onFailure(new RuleEngineException(t.getMessage()));
    }
}
