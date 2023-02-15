package org.thingsboard.server.queue;

public interface TbQueueCallback {

    void onSuccess(TbQueueMsgMetadata metadata);

    void onFailure(Throwable t);
}
