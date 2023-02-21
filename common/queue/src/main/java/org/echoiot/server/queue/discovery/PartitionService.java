package org.echoiot.server.queue.discovery;

import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Once application is ready or cluster topology changes, this Service will produce {@link PartitionChangeEvent}
 */
public interface PartitionService {

    TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId);

    TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    /**
     * Received from the Discovery service when network topology is changed.
     * @param currentService - current service information {@link org.echoiot.server.gen.transport.TransportProtos.ServiceInfo}
     * @param otherServices - all other discovered services {@link org.echoiot.server.gen.transport.TransportProtos.ServiceInfo}
     */
    void recalculatePartitions(TransportProtos.ServiceInfo currentService, List<TransportProtos.ServiceInfo> otherServices);

    /**
     * Get all active service ids by service type
     * @param serviceType to filter the list of services
     * @return list of all active services
     */
    Set<String> getAllServiceIds(ServiceType serviceType);

    Set<TransportProtos.ServiceInfo> getAllServices(ServiceType serviceType);

    Set<TransportProtos.ServiceInfo> getOtherServices(ServiceType serviceType);

    int resolvePartitionIndex(UUID entityId, int partitions);

    void removeTenant(TenantId tenantId);

    int countTransportsByType(String type);

    void updateQueue(TransportProtos.QueueUpdateMsg queueUpdateMsg);

    void removeQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg);
}
