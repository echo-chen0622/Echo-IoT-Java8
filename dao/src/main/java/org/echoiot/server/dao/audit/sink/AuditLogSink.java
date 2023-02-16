package org.echoiot.server.dao.audit.sink;

import org.echoiot.server.common.data.audit.AuditLog;

public interface AuditLogSink {

    void logAction(AuditLog auditLogEntry);
}
