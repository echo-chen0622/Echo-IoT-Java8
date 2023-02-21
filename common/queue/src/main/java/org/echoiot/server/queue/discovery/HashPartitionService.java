package org.echoiot.server.queue.discovery;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.ServiceInfo;
import org.echoiot.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.echoiot.server.queue.discovery.event.ServiceListChangedEvent;
import org.echoiot.server.queue.util.AfterStartUp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HashPartitionService implements PartitionService {

    @Value("${queue.core.topic}")
    private String coreTopic;
    @Value("${queue.core.partitions:100}")
    private Integer corePartitions;
    @Value("${queue.vc.topic:tb_version_control}")
    private String vcTopic;
    @Value("${queue.vc.partitions:10}")
    private Integer vcPartitions;
    @Value("${queue.partitions.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TenantRoutingInfoService tenantRoutingInfoService;
    private final QueueRoutingInfoService queueRoutingInfoService;

    @NotNull
    private ConcurrentMap<QueueKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();

    private final ConcurrentMap<QueueKey, String> partitionTopicsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<QueueKey, Integer> partitionSizesMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, TenantRoutingInfo> tenantRoutingInfoMap = new ConcurrentHashMap<>();

    private final Map<String, List<ServiceInfo>> tbTransportServicesByType = new HashMap<>();
    private List<ServiceInfo> currentOtherServices;

    private HashFunction hashFunction;

    public HashPartitionService(TbServiceInfoProvider serviceInfoProvider,
                                TenantRoutingInfoService tenantRoutingInfoService,
                                ApplicationEventPublisher applicationEventPublisher,
                                QueueRoutingInfoService queueRoutingInfoService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.tenantRoutingInfoService = tenantRoutingInfoService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.queueRoutingInfoService = queueRoutingInfoService;
    }

    @PostConstruct
    public void init() {
        this.hashFunction = forName(hashFunctionName);
        @NotNull QueueKey coreKey = new QueueKey(ServiceType.TB_CORE);
        partitionSizesMap.put(coreKey, corePartitions);
        partitionTopicsMap.put(coreKey, coreTopic);

        @NotNull QueueKey vcKey = new QueueKey(ServiceType.TB_VC_EXECUTOR);
        partitionSizesMap.put(vcKey, vcPartitions);
        partitionTopicsMap.put(vcKey, vcTopic);

        if (!isTransport(serviceInfoProvider.getServiceType())) {
            doInitRuleEnginePartitions();
        }
    }

    @AfterStartUp(order = AfterStartUp.QUEUE_INFO_INITIALIZATION)
    public void partitionsInit() {
        if (isTransport(serviceInfoProvider.getServiceType())) {
            doInitRuleEnginePartitions();
        }
    }

    private void doInitRuleEnginePartitions() {
        List<QueueRoutingInfo> queueRoutingInfoList = getQueueRoutingInfos();
        queueRoutingInfoList.forEach(queue -> {
            @NotNull QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
            partitionTopicsMap.put(queueKey, queue.getQueueTopic());
            partitionSizesMap.put(queueKey, queue.getPartitions());
        });
    }

    private List<QueueRoutingInfo> getQueueRoutingInfos() {
        List<QueueRoutingInfo> queueRoutingInfoList;
        String serviceType = serviceInfoProvider.getServiceType();

        if (isTransport(serviceType)) {
            //If transport started earlier than tb-core
            int getQueuesRetries = 10;
            while (true) {
                if (getQueuesRetries > 0) {
                    log.info("Try to get queue routing info.");
                    try {
                        queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
                        break;
                    } catch (Exception e) {
                        log.info("Failed to get queues routing info: {}!", e.getMessage());
                        getQueuesRetries--;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        log.info("Failed to await queues routing info!", e);
                    }
                } else {
                    throw new RuntimeException("Failed to await queues routing info!");
                }
            }
        } else {
            queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
        }
        return queueRoutingInfoList;
    }

    private boolean isTransport(String serviceType) {
        return "tb-transport".equals(serviceType);
    }

    @Override
    public void updateQueue(@NotNull TransportProtos.QueueUpdateMsg queueUpdateMsg) {
        @NotNull TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
        @NotNull QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName(), tenantId);
        partitionTopicsMap.put(queueKey, queueUpdateMsg.getQueueTopic());
        partitionSizesMap.put(queueKey, queueUpdateMsg.getPartitions());
        myPartitions.remove(queueKey);
    }

    @Override
    public void removeQueue(@NotNull TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        @NotNull TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
        @NotNull QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
        myPartitions.remove(queueKey);
        partitionTopicsMap.remove(queueKey);
        partitionSizesMap.remove(queueKey);
        //TODO: remove after merging tb entity services
        removeTenant(tenantId);
    }

    @Override
    public TopicPartitionInfo resolve(@NotNull ServiceType serviceType, String queueName, TenantId tenantId, @NotNull EntityId entityId) {
        TenantId isolatedOrSystemTenantId = getIsolatedOrSystemTenantId(serviceType, tenantId);
        @NotNull QueueKey queueKey = new QueueKey(serviceType, queueName, isolatedOrSystemTenantId);
        if (!partitionSizesMap.containsKey(queueKey)) {
            queueKey = new QueueKey(serviceType, isolatedOrSystemTenantId);
        }
        return resolve(queueKey, entityId);
    }

    @Override
    public TopicPartitionInfo resolve(@NotNull ServiceType serviceType, TenantId tenantId, @NotNull EntityId entityId) {
        return resolve(serviceType, null, tenantId, entityId);
    }

    private TopicPartitionInfo resolve(@NotNull QueueKey queueKey, @NotNull EntityId entityId) {
        int hash = hashFunction.newHasher()
                .putLong(entityId.getId().getMostSignificantBits())
                .putLong(entityId.getId().getLeastSignificantBits()).hash().asInt();

        Integer partitionSize = partitionSizesMap.get(queueKey);
        int partition = Math.abs(hash % partitionSize);

        return buildTopicPartitionInfo(queueKey, partition);
    }

    @Override
    public synchronized void recalculatePartitions(@NotNull ServiceInfo currentService, @NotNull List<ServiceInfo> otherServices) {
        tbTransportServicesByType.clear();
        logServiceInfo(currentService);
        otherServices.forEach(this::logServiceInfo);

        @NotNull Map<QueueKey, List<ServiceInfo>> queueServicesMap = new HashMap<>();
        addNode(queueServicesMap, currentService);
        for (@NotNull ServiceInfo other : otherServices) {
            addNode(queueServicesMap, other);
        }
        queueServicesMap.values().forEach(list -> list.sort(Comparator.comparing(ServiceInfo::getServiceId)));

        ConcurrentMap<QueueKey, List<Integer>> oldPartitions = myPartitions;
        myPartitions = new ConcurrentHashMap<>();
        partitionSizesMap.forEach((queueKey, size) -> {
            for (int i = 0; i < size; i++) {
                ServiceInfo serviceInfo = resolveByPartitionIdx(queueServicesMap.get(queueKey), queueKey, i);
                if (currentService.equals(serviceInfo)) {
                    myPartitions.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(i);
                }
            }
        });

        oldPartitions.forEach((queueKey, partitions) -> {
            if (!myPartitions.containsKey(queueKey)) {
                log.info("[{}] NO MORE PARTITIONS FOR CURRENT KEY", queueKey);
                applicationEventPublisher.publishEvent(new PartitionChangeEvent(this, queueKey, Collections.emptySet()));
            }
        });

        myPartitions.forEach((queueKey, partitions) -> {
            if (!partitions.equals(oldPartitions.get(queueKey))) {
                log.info("[{}] NEW PARTITIONS: {}", queueKey, partitions);
                @NotNull Set<TopicPartitionInfo> tpiList = partitions.stream()
                                                                     .map(partition -> buildTopicPartitionInfo(queueKey, partition))
                                                                     .collect(Collectors.toSet());
                applicationEventPublisher.publishEvent(new PartitionChangeEvent(this, queueKey, tpiList));
            }
        });

        if (currentOtherServices == null) {
            currentOtherServices = new ArrayList<>(otherServices);
        } else {
            @NotNull Set<QueueKey> changes = new HashSet<>();
            @NotNull Map<QueueKey, List<ServiceInfo>> currentMap = getServiceKeyListMap(currentOtherServices);
            @NotNull Map<QueueKey, List<ServiceInfo>> newMap = getServiceKeyListMap(otherServices);
            currentOtherServices = otherServices;
            currentMap.forEach((key, list) -> {
                if (!list.equals(newMap.get(key))) {
                    changes.add(key);
                }
            });
            currentMap.keySet().forEach(newMap::remove);
            changes.addAll(newMap.keySet());
            if (!changes.isEmpty()) {
                applicationEventPublisher.publishEvent(new ClusterTopologyChangeEvent(this, changes));
            }
        }

        applicationEventPublisher.publishEvent(new ServiceListChangedEvent(otherServices, currentService));
    }

    @NotNull
    @Override
    public Set<String> getAllServiceIds(@NotNull ServiceType serviceType) {
        return getAllServices(serviceType).stream().map(ServiceInfo::getServiceId).collect(Collectors.toSet());
    }

    @Override
    public Set<ServiceInfo> getAllServices(@NotNull ServiceType serviceType) {
        @NotNull Set<ServiceInfo> result = getOtherServices(serviceType);
        ServiceInfo current = serviceInfoProvider.getServiceInfo();
        if (current.getServiceTypesList().contains(serviceType.name())) {
            result.add(current);
        }
        return result;
    }

    @NotNull
    @Override
    public Set<ServiceInfo> getOtherServices(@NotNull ServiceType serviceType) {
        @NotNull Set<ServiceInfo> result = new HashSet<>();
        if (currentOtherServices != null) {
            for (@NotNull ServiceInfo serviceInfo : currentOtherServices) {
                if (serviceInfo.getServiceTypesList().contains(serviceType.name())) {
                    result.add(serviceInfo);
                }
            }
        }
        return result;
    }


    @Override
    public int resolvePartitionIndex(@NotNull UUID entityId, int partitions) {
        int hash = hashFunction.newHasher()
                .putLong(entityId.getMostSignificantBits())
                .putLong(entityId.getLeastSignificantBits()).hash().asInt();
        return Math.abs(hash % partitions);
    }

    @Override
    public void removeTenant(TenantId tenantId) {
        tenantRoutingInfoMap.remove(tenantId);
    }

    @Override
    public int countTransportsByType(String type) {
        var list = tbTransportServicesByType.get(type);
        return list == null ? 0 : list.size();
    }

    @NotNull
    private Map<QueueKey, List<ServiceInfo>> getServiceKeyListMap(@NotNull List<ServiceInfo> services) {
        @NotNull final Map<QueueKey, List<ServiceInfo>> currentMap = new HashMap<>();
        services.forEach(serviceInfo -> {
            for (@NotNull String serviceTypeStr : serviceInfo.getServiceTypesList()) {
                @NotNull ServiceType serviceType = ServiceType.valueOf(serviceTypeStr.toUpperCase());
                if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                    partitionTopicsMap.keySet().forEach(queueKey ->
                            currentMap.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(serviceInfo));
                } else {
                    @NotNull QueueKey queueKey = new QueueKey(serviceType);
                    currentMap.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(serviceInfo);
                }
            }
        });
        return currentMap;
    }

    private TopicPartitionInfo buildTopicPartitionInfo(@NotNull QueueKey queueKey, int partition) {
        TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
        tpi.topic(partitionTopicsMap.get(queueKey));
        tpi.partition(partition);
        tpi.tenantId(queueKey.getTenantId());

        List<Integer> partitions = myPartitions.get(queueKey);
        if (partitions != null) {
            tpi.myPartition(partitions.contains(partition));
        } else {
            tpi.myPartition(false);
        }
        return tpi.build();
    }

    private boolean isIsolated(@NotNull ServiceType serviceType, TenantId tenantId) {
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return false;
        }
        TenantRoutingInfo routingInfo = tenantRoutingInfoMap.get(tenantId);
        if (routingInfo == null) {
            synchronized (tenantRoutingInfoMap) {
                routingInfo = tenantRoutingInfoMap.get(tenantId);
                if (routingInfo == null) {
                    routingInfo = tenantRoutingInfoService.getRoutingInfo(tenantId);
                    tenantRoutingInfoMap.put(tenantId, routingInfo);
                }
            }
        }
        if (routingInfo == null) {
            throw new RuntimeException("Tenant not found!");
        }
        if (Objects.requireNonNull(serviceType) == ServiceType.TB_RULE_ENGINE) {
            return routingInfo.isIsolatedTbRuleEngine();
        }
        return false;
    }

    private TenantId getIsolatedOrSystemTenantId(@NotNull ServiceType serviceType, TenantId tenantId) {
        return isIsolated(serviceType, tenantId) ? tenantId : TenantId.SYS_TENANT_ID;
    }

    private void logServiceInfo(@NotNull TransportProtos.ServiceInfo server) {
        log.info("[{}] Found common server: [{}]", server.getServiceId(), server.getServiceTypesList());
    }

    private void addNode(@NotNull Map<QueueKey, List<ServiceInfo>> queueServiceList, @NotNull ServiceInfo instance) {
        for (@NotNull String serviceTypeStr : instance.getServiceTypesList()) {
            @NotNull ServiceType serviceType = ServiceType.valueOf(serviceTypeStr.toUpperCase());
            if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                partitionTopicsMap.keySet().forEach(key -> {
                    if (key.getType().equals(ServiceType.TB_RULE_ENGINE)) {
                        queueServiceList.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
                    }
                });
            } else if (ServiceType.TB_CORE.equals(serviceType) || ServiceType.TB_VC_EXECUTOR.equals(serviceType)) {
                queueServiceList.computeIfAbsent(new QueueKey(serviceType), key -> new ArrayList<>()).add(instance);
            }
        }

        for (String transportType : instance.getTransportsList()) {
            tbTransportServicesByType.computeIfAbsent(transportType, t -> new ArrayList<>()).add(instance);
        }
    }

    protected ServiceInfo resolveByPartitionIdx(@Nullable List<ServiceInfo> servers, @NotNull QueueKey queueKey, int partition) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        if (!ServiceType.TB_RULE_ENGINE.equals(queueKey.getType()) || TenantId.SYS_TENANT_ID.equals(queueKey.getTenantId())) {
            return servers.get(partition % servers.size());
        } else {
            int hash = hashFunction.newHasher().putLong(queueKey.getTenantId().getId().getMostSignificantBits())
                    .putLong(queueKey.getTenantId().getId().getLeastSignificantBits())
                    .putString(queueKey.getQueueName(), StandardCharsets.UTF_8)
                    .hash().asInt();

            return servers.get(Math.abs((hash + partition) % servers.size()));
        }
    }

    @NotNull
    public static HashFunction forName(@NotNull String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "sha256":
                return Hashing.sha256();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

}
