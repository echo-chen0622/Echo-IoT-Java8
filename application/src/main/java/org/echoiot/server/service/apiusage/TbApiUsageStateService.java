package org.echoiot.server.service.apiusage;

import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.stats.TbApiUsageStateClient;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.springframework.context.ApplicationListener;
import org.thingsboard.rule.engine.api.RuleEngineApiUsageStateService;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;

public interface TbApiUsageStateService extends TbApiUsageStateClient, RuleEngineApiUsageStateService, ApplicationListener<PartitionChangeEvent> {

    void process(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback);

    void onTenantProfileUpdate(TenantProfileId tenantProfileId);

    void onTenantUpdate(TenantId tenantId);

    void onTenantDelete(TenantId tenantId);

    void onCustomerDelete(CustomerId customerId);

    void onApiUsageStateUpdate(TenantId tenantId);
}
