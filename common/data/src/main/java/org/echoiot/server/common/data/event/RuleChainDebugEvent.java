package org.echoiot.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class RuleChainDebugEvent extends Event {

    private static final long serialVersionUID = -386392236201116767L;

    @Builder
    private RuleChainDebugEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts, String message, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.message = message;
        this.error = error;
    }

    @Getter
    @Setter
    private String message;
    @Getter
    @Setter
    private String error;

    @NotNull
    @Override
    public EventType getType() {
        return EventType.DEBUG_RULE_CHAIN;
    }

    @NotNull
    @Override
    public EventInfo toInfo(@NotNull EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        putNotNull(json, "message", message);
        putNotNull(json, "error", error);
        return eventInfo;
    }
}
