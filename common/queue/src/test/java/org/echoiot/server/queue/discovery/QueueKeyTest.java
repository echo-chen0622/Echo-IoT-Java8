package org.echoiot.server.queue.discovery;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.queue.ServiceType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class QueueKeyTest {

    @Test
    void testToStringSystemTenant() {
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TenantId.SYS_TENANT_ID);
        log.info("The queue key is {}",queueKey);
        assertThat(queueKey.toString()).isEqualTo("QK(Main,TB_RULE_ENGINE,system)");
    }

    @Test
    void testToStringCustomTenant() {
        TenantId tenantId = TenantId.fromUUID(UUID.fromString("3ebd39eb-43d4-4911-a818-cdbf8d508f88"));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, tenantId);
        log.info("The queue key is {}",queueKey);
        assertThat(queueKey.toString()).isEqualTo("QK(Main,TB_RULE_ENGINE,3ebd39eb-43d4-4911-a818-cdbf8d508f88)");
    }
}
