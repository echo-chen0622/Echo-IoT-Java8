package org.echoiot.server.service.queue;

import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.springframework.context.ApplicationListener;

public interface TbCoreConsumerService extends ApplicationListener<PartitionChangeEvent> {

}