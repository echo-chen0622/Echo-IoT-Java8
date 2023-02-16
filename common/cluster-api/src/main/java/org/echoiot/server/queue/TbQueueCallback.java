package org.echoiot.server.queue;

public interface TbQueueCallback {

    void onSuccess(TbQueueMsgMetadata metadata);

    void onFailure(Throwable t);
}
