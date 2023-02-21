package org.echoiot.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public final class EdgeUtils {

    private static final int STACK_TRACE_LIMIT = 10;

    private EdgeUtils() {
    }

    @Nullable
    public static EdgeEventType getEdgeEventTypeByEntityType(@NotNull EntityType entityType) {
        switch (entityType) {
            case EDGE:
                return EdgeEventType.EDGE;
            case DEVICE:
                return EdgeEventType.DEVICE;
            case DEVICE_PROFILE:
                return EdgeEventType.DEVICE_PROFILE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ASSET_PROFILE:
                return EdgeEventType.ASSET_PROFILE;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            case DASHBOARD:
                return EdgeEventType.DASHBOARD;
            case USER:
                return EdgeEventType.USER;
            case RULE_CHAIN:
                return EdgeEventType.RULE_CHAIN;
            case ALARM:
                return EdgeEventType.ALARM;
            case TENANT:
                return EdgeEventType.TENANT;
            case CUSTOMER:
                return EdgeEventType.CUSTOMER;
            case WIDGETS_BUNDLE:
                return EdgeEventType.WIDGETS_BUNDLE;
            case WIDGET_TYPE:
                return EdgeEventType.WIDGET_TYPE;
            case OTA_PACKAGE:
                return EdgeEventType.OTA_PACKAGE;
            case QUEUE:
                return EdgeEventType.QUEUE;
            default:
                log.warn("Unsupported entity type [{}]", entityType);
                return null;
        }
    }

    public static int nextPositiveInt() {
        return ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    @NotNull
    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               @Nullable EntityId entityId,
                                               JsonNode body) {
        @NotNull EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setBody(body);
        return edgeEvent;
    }

    @NotNull
    public static String createErrorMsgFromRootCauseAndStackTrace(@NotNull Throwable t) {
        @NotNull Throwable rootCause = Throwables.getRootCause(t);
        @NotNull StringBuilder errorMsg = new StringBuilder(rootCause.getMessage() != null ? rootCause.getMessage() : "");
        if (rootCause.getStackTrace().length > 0) {
            int idx = 0;
            for (@NotNull StackTraceElement stackTraceElement : rootCause.getStackTrace()) {
                errorMsg.append("\n").append(stackTraceElement);
                idx++;
                if (idx > STACK_TRACE_LIMIT) {
                    break;
                }
            }
        }
        return errorMsg.toString();
    }
}
