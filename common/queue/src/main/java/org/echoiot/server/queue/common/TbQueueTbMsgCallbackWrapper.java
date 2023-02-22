package org.echoiot.server.queue.common;

import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsgMetadata;

public class TbQueueTbMsgCallbackWrapper implements TbQueueCallback {

    private final TbMsgCallback tbMsgCallback;

    public TbQueueTbMsgCallbackWrapper(TbMsgCallback tbMsgCallback) {
        this.tbMsgCallback = tbMsgCallback;
    }

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        tbMsgCallback.onSuccess();
    }

    @Override
    public void onFailure(Throwable t) {
        tbMsgCallback.onFailure(new RuleEngineException(t.getMessage()));
    }
}
