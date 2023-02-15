package org.thingsboard.server.dao.audit.sink;

import org.thingsboard.server.common.data.audit.AuditLog;

public interface AuditLogSink {

    void logAction(AuditLog auditLogEntry);
}
