package org.echoiot.server.vc.service;

import org.echoiot.server.queue.discovery.QueueRoutingInfo;
import org.echoiot.server.queue.discovery.QueueRoutingInfoService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class VersionControlQueueRoutingInfoService implements QueueRoutingInfoService {
    @NotNull
    @Override
    public List<QueueRoutingInfo> getAllQueuesRoutingInfo() {
        return Collections.emptyList();
    }
}
