package org.thingsboard.server.queue;

import org.thingsboard.server.common.data.queue.Queue;

public interface TbQueueClusterService {
    void onQueueChange(Queue queue);

    void onQueueDelete(Queue queue);
}
