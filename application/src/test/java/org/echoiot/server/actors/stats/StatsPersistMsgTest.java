package org.echoiot.server.actors.stats;

import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatsPersistMsgTest {

    @Test
    void testIsEmpty() {
        @NotNull StatsPersistMsg emptyStats = new StatsPersistMsg(0, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
        assertThat(emptyStats.isEmpty()).isTrue();
    }

    @Test
    void testNotEmpty() {
        assertThat(new StatsPersistMsg(1, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isEmpty()).isFalse();
        assertThat(new StatsPersistMsg(0, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isEmpty()).isFalse();
        assertThat(new StatsPersistMsg(1, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isEmpty()).isFalse();
    }

}
