package org.echoiot.server.common.stats;

import org.echoiot.server.common.data.ApiUsageRecordKey;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;

public interface TbApiUsageReportClient {

    void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value);

    void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key);

}
