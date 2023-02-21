package org.echoiot.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.audit.AuditLogDao;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.echoiot.server.queue.discovery.PartitionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnExpression("${sql.ttl.audit_logs.enabled:true} && ${sql.ttl.audit_logs.ttl:0} > 0")
@Slf4j
public class AuditLogsCleanUpService extends AbstractCleanUpService {

    private final AuditLogDao auditLogDao;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.ttl.audit_logs.ttl:0}")
    private long ttlInSec;
    @Value("${sql.audit_logs.partition_size:168}")
    private int partitionSizeInHours;

    public AuditLogsCleanUpService(PartitionService partitionService, AuditLogDao auditLogDao, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.auditLogDao = auditLogDao;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.audit_logs.checking_interval_ms})}",
            fixedDelayString = "${sql.ttl.audit_logs.checking_interval_ms}")
    public void cleanUp() {
        long auditLogsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        if (isSystemTenantPartitionMine()) {
            auditLogDao.cleanUpAuditLogs(auditLogsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(ModelConstants.AUDIT_LOG_COLUMN_FAMILY_NAME, auditLogsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
