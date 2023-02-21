package org.echoiot.server.service.ttl.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.rpc.RpcDao;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.queue.discovery.PartitionService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.msg.queue.ServiceType;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class RpcCleanUpService {
    @Value("${sql.ttl.rpc.enabled}")
    private boolean ttlTaskExecutionEnabled;

    @NotNull
    private final TenantService tenantService;
    @NotNull
    private final PartitionService partitionService;
    @NotNull
    private final TbTenantProfileCache tenantProfileCache;
    @NotNull
    private final RpcDao rpcDao;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.rpc.checking_interval})}", fixedDelayString = "${sql.ttl.rpc.checking_interval}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled) {
            PageLink tenantsBatchRequest = new PageLink(10_000, 0);
            PageData<TenantId> tenantsIds;
            do {
                tenantsIds = tenantService.findTenantsIds(tenantsBatchRequest);
                for (TenantId tenantId : tenantsIds.getData()) {
                    if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
                        continue;
                    }

                    @NotNull Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
                    if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getRpcTtlDays() == 0) {
                        continue;
                    }

                    long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getRpcTtlDays());
                    long expirationTime = System.currentTimeMillis() - ttl;

                    long totalRemoved = rpcDao.deleteOutdatedRpcByTenantId(tenantId, expirationTime);

                    if (totalRemoved > 0) {
                        log.info("Removed {} outdated rpc(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
                    }
                }

                tenantsBatchRequest = tenantsBatchRequest.nextPageLink();
            } while (tenantsIds.hasNext());
        }
    }

}
