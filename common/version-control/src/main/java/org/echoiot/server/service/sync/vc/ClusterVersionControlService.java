package org.echoiot.server.service.sync.vc;

import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.springframework.context.ApplicationListener;

public interface ClusterVersionControlService extends ApplicationListener<PartitionChangeEvent> {
}
