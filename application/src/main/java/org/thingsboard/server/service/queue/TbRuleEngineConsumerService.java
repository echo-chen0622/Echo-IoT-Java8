package org.thingsboard.server.service.queue;

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

public interface TbRuleEngineConsumerService extends ApplicationListener<PartitionChangeEvent> {

}
