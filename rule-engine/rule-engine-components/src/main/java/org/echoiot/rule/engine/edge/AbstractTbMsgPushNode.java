package org.echoiot.rule.engine.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class AbstractTbMsgPushNode<T extends BaseTbMsgPushNodeConfiguration, S, U> implements TbNode {

    protected T config;

    private static final String SCOPE = "scope";

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, getConfigClazz());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (getIgnoredMessageSource().equalsIgnoreCase(msg.getMetaData().getValue(DataConstants.MSG_SOURCE_KEY))) {
            log.debug("Ignoring msg from the {}, msg [{}]", getIgnoredMessageSource(), msg);
            ctx.ack(msg);
            return;
        }
        if (isSupportedOriginator(msg.getOriginator().getEntityType())) {
            if (isSupportedMsgType(msg.getType())) {
                processMsg(ctx, msg);
            } else {
                String errMsg = String.format("Unsupported msg type %s", msg.getType());
                log.debug(errMsg);
                ctx.tellFailure(msg, new RuntimeException(errMsg));
            }
        } else {
            String errMsg = String.format("Unsupported originator type %s", msg.getOriginator().getEntityType());
            log.debug(errMsg);
            ctx.tellFailure(msg, new RuntimeException(errMsg));
        }
    }

    protected S buildEvent(TbMsg msg, TbContext ctx) {
        String msgType = msg.getType();
        if (DataConstants.ALARM.equals(msgType)) {
            return buildEvent(ctx.getTenantId(), EdgeEventActionType.ADDED, getUUIDFromMsgData(msg), getAlarmEventType(), null);
        } else {
            EdgeEventActionType actionType = getEdgeEventActionTypeByMsgType(msgType);
            Map<String, Object> entityBody = new HashMap<>();
            Map<String, String> metadata = msg.getMetaData().getData();
            JsonNode dataJson = JacksonUtil.toJsonNode(msg.getData());
            switch (actionType) {
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                    entityBody.put("kv", dataJson);
                    entityBody.put(SCOPE, getScope(metadata));
                    if (EdgeEventActionType.POST_ATTRIBUTES.equals(actionType)) {
                        entityBody.put("isPostAttributes", true);
                    }
                    break;
                case ATTRIBUTES_DELETED:
                    @Nullable List<String> keys = JacksonUtil.convertValue(dataJson.get("attributes"), new TypeReference<>() {});
                    entityBody.put("keys", keys);
                    entityBody.put(SCOPE, getScope(metadata));
                    break;
                case TIMESERIES_UPDATED:
                    entityBody.put("data", dataJson);
                    entityBody.put("ts", msg.getMetaDataTs());
                    break;
            }
            return buildEvent(ctx.getTenantId(),
                    actionType,
                    msg.getOriginator().getId(),
                    getEventTypeByEntityType(msg.getOriginator().getEntityType()),
                    JacksonUtil.valueToTree(entityBody));
        }
    }

    @Nullable
    abstract S buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, U eventType, JsonNode entityBody);

    @Nullable
    abstract U getEventTypeByEntityType(EntityType entityType);

    @Nullable
    abstract U getAlarmEventType();

    @Nullable
    abstract String getIgnoredMessageSource();

    abstract protected Class<T> getConfigClazz();

    abstract void processMsg(TbContext ctx, TbMsg msg);

    protected UUID getUUIDFromMsgData(TbMsg msg) {
        JsonNode data = JacksonUtil.toJsonNode(msg.getData()).get("id");
        @Nullable String id = JacksonUtil.convertValue(data.get("id"), String.class);
        return UUID.fromString(id);
    }

    protected String getScope(Map<String, String> metadata) {
        String scope = metadata.get(SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = config.getScope();
        }
        return scope;
    }

    protected EdgeEventActionType getEdgeEventActionTypeByMsgType(String msgType) {
        EdgeEventActionType actionType;
        if (SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)
                || DataConstants.TIMESERIES_UPDATED.equals(msgType)) {
            actionType = EdgeEventActionType.TIMESERIES_UPDATED;
        } else if (DataConstants.ATTRIBUTES_UPDATED.equals(msgType)) {
            actionType = EdgeEventActionType.ATTRIBUTES_UPDATED;
        } else if (SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)) {
            actionType = EdgeEventActionType.POST_ATTRIBUTES;
        } else if (DataConstants.ATTRIBUTES_DELETED.equals(msgType)) {
            actionType = EdgeEventActionType.ATTRIBUTES_DELETED;
        } else {
            log.warn("Unsupported msg type [{}]", msgType);
            throw new IllegalArgumentException("Unsupported msg type: " + msgType);
        }
        return actionType;
    }

    protected boolean isSupportedMsgType(String msgType) {
        return SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)
                || SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)
                || DataConstants.ATTRIBUTES_UPDATED.equals(msgType)
                || DataConstants.ATTRIBUTES_DELETED.equals(msgType)
                || DataConstants.TIMESERIES_UPDATED.equals(msgType)
                || DataConstants.ALARM.equals(msgType);
    }

    protected boolean isSupportedOriginator(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
            case TENANT:
            case CUSTOMER:
            case USER:
            case EDGE:
                return true;
            default:
                return false;
        }
    }
}
