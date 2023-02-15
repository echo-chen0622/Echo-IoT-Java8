package org.thingsboard.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmsCleanUpService {

    @Value("${sql.ttl.alarms.removal_batch_size}")
    private Integer removalBatchSize;

    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    private final AlarmService alarmService;
    private final RelationService relationService;
    private final EntityActionService entityActionService;
    private final PartitionService partitionService;
    private final TbTenantProfileCache tenantProfileCache;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.alarms.checking_interval})}", fixedDelayString = "${sql.ttl.alarms.checking_interval}")
    public void cleanUp() {
        PageLink tenantsBatchRequest = new PageLink(10_000, 0);
        PageLink removalBatchRequest = new PageLink(removalBatchSize, 0 );
        PageData<TenantId> tenantsIds;
        do {
            tenantsIds = tenantService.findTenantsIds(tenantsBatchRequest);
            for (TenantId tenantId : tenantsIds.getData()) {
                if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
                    continue;
                }

                Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
                if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getAlarmsTtlDays() == 0) {
                    continue;
                }

                long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getAlarmsTtlDays());
                long expirationTime = System.currentTimeMillis() - ttl;

                long totalRemoved = 0;
                while (true) {
                    PageData<AlarmId> toRemove = alarmDao.findAlarmsIdsByEndTsBeforeAndTenantId(expirationTime, tenantId, removalBatchRequest);
                    toRemove.getData().forEach(alarmId -> {
                        relationService.deleteEntityRelations(tenantId, alarmId);
                        Alarm alarm = alarmService.deleteAlarm(tenantId, alarmId).getAlarm();
                        entityActionService.pushEntityActionToRuleEngine(alarm.getOriginator(), alarm, tenantId, null, ActionType.ALARM_DELETE, null);
                    });

                    totalRemoved += toRemove.getTotalElements();
                    if (!toRemove.hasNext()) {
                        break;
                    }
                }

                if (totalRemoved > 0) {
                    log.info("Removed {} outdated alarm(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
                }
            }

            tenantsBatchRequest = tenantsBatchRequest.nextPageLink();
        } while (tenantsIds.hasNext());
    }

}
