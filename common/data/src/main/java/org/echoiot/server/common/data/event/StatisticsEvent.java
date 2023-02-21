package org.echoiot.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class StatisticsEvent extends Event {

    private static final long serialVersionUID = 6683733979448910631L;

    @Builder
    private StatisticsEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts, long messagesProcessed, long errorsOccurred) {
        super(tenantId, entityId, serviceId, id, ts);
        this.messagesProcessed = messagesProcessed;
        this.errorsOccurred = errorsOccurred;
    }

    @Getter
    private final long messagesProcessed;
    @Getter
    private final long errorsOccurred;

    @NotNull
    @Override
    public EventType getType() {
        return EventType.STATS;
    }

    @NotNull
    @Override
    public EventInfo toInfo(@NotNull EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("messagesProcessed", messagesProcessed).put("errorsOccurred", errorsOccurred);
        return eventInfo;
    }
}
