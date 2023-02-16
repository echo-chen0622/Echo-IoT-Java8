package org.echoiot.server.dao.audit;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.HasName;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.audit.AuditLog;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "false")
public class DummyAuditLogServiceImpl implements AuditLogService {

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return new PageData<>();
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndUserId(TenantId tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return new PageData<>();
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return new PageData<>();
    }

    @Override
    public PageData<AuditLog> findAuditLogsByTenantId(TenantId tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return new PageData<>();
    }

    @Override
    public <E extends HasName, I extends EntityId> ListenableFuture<List<Void>> logEntityAction(TenantId tenantId, CustomerId customerId, UserId userId, String userName, I entityId, E entity, ActionType actionType, Exception e, Object... additionalInfo) {
        return null;
    }
}