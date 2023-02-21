package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AuditLogId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AuditLogId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static AuditLogId fromString(@NotNull String auditLogId) {
        return new AuditLogId(UUID.fromString(auditLogId));
    }
}
