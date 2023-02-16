package org.echoiot.server.queue;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.stats.MessagesStats;

public interface TbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    void init();

    ListenableFuture<Response> send(Request request);

    ListenableFuture<Response> send(Request request, long timeoutNs);

    void stop();

    void setMessagesStats(MessagesStats messagesStats);
}
