package org.thingsboard.server.service.sync.vc;

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

public interface ClusterVersionControlService extends ApplicationListener<PartitionChangeEvent> {
}
