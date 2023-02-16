package org.echoiot.server.dao.audit;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.audit.AuditLog;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface AuditLogDao extends Dao<AuditLog> {

    ListenableFuture<Void> saveByTenantId(AuditLog auditLog);

    PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink);

    PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink);

    PageData<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink);

    PageData<AuditLog> findAuditLogsByTenantId(UUID tenantId, List<ActionType> actionTypes, TimePageLink pageLink);

    void cleanUpAuditLogs(long expTime);

    void migrateAuditLogs();

}
