package org.echoiot.server.dao.audit;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.audit.ActionType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogLevelFilter {

    private final Map<EntityType, AuditLogLevelMask> entityTypeMask = new HashMap<>();

    public AuditLogLevelFilter(@NotNull AuditLogLevelProperties auditLogLevelProperties) {
        Map<String, String> mask = auditLogLevelProperties.getMask();
        entityTypeMask.clear();
        mask.forEach((entityTypeStr, logLevelMaskStr) -> {
            @NotNull EntityType entityType = EntityType.valueOf(entityTypeStr.toUpperCase(Locale.ENGLISH));
            @NotNull AuditLogLevelMask logLevelMask = AuditLogLevelMask.valueOf(logLevelMaskStr.toUpperCase());
            entityTypeMask.put(entityType, logLevelMask);
        });
    }

    public boolean logEnabled(EntityType entityType, @NotNull ActionType actionType) {
        AuditLogLevelMask logLevelMask = entityTypeMask.get(entityType);
        if (logLevelMask != null) {
            return actionType.isRead() ? logLevelMask.isRead() : logLevelMask.isWrite();
        } else {
            return false;
        }
    }

}
