package org.echoiot.server.queue.discovery.event;

import lombok.Getter;
import org.echoiot.server.queue.discovery.QueueKey;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ClusterTopologyChangeEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -2441739930040282254L;

    @Getter
    private final Set<QueueKey> queueKeys;

    public ClusterTopologyChangeEvent(@NotNull Object source, Set<QueueKey> queueKeys) {
        super(source);
        this.queueKeys = queueKeys;
    }
}
