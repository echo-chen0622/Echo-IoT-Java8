package org.echoiot.server.queue;

import org.echoiot.server.common.data.queue.Queue;

public interface TbQueueClusterService {
    void onQueueChange(Queue queue);

    void onQueueDelete(Queue queue);
}
