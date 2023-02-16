package org.echoiot.server.queue;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by Echo on 05.10.18.
 */
public interface TbQueueHandler<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    ListenableFuture<Response> handle(Request request);

}
