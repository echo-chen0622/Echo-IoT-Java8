package org.thingsboard.server.common.msg.queue;

import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicPartitionInfoTest {

    @Test
    public void givenTopicPartitionInfo_whenEquals_thenTrue() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()));

    }

    @Test
    public void givenTopicPartitionInfo_whenEquals_thenFalse() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

    }
}
