package org.thingsboard.server.service.subscription;

import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.telemetry.sub.TelemetrySubscriptionUpdate;

public interface TbLocalSubscriptionService {

    void addSubscription(TbSubscription subscription);

    void cancelSubscription(String sessionId, int subscriptionId);

    void cancelAllSessionSubscriptions(String sessionId);

    void onSubscriptionUpdate(String sessionId, TelemetrySubscriptionUpdate update, TbCallback callback);

    void onSubscriptionUpdate(String sessionId, AlarmSubscriptionUpdate update, TbCallback callback);

    void onApplicationEvent(PartitionChangeEvent event);

    void onApplicationEvent(ClusterTopologyChangeEvent event);
}
