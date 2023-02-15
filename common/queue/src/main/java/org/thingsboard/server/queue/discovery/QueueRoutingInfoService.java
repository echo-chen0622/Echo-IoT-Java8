package org.thingsboard.server.queue.discovery;

import java.util.List;

public interface QueueRoutingInfoService {

    List<QueueRoutingInfo> getAllQueuesRoutingInfo();

}
