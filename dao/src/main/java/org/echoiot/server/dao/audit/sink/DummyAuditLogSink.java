package org.echoiot.server.dao.audit.sink;

import org.echoiot.server.common.data.audit.AuditLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "none")
public class DummyAuditLogSink implements AuditLogSink {

    @Override
    public void logAction(AuditLog auditLogEntry) {
    }
}
