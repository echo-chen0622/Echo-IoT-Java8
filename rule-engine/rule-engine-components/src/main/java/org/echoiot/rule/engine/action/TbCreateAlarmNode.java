package org.echoiot.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create alarm", relationTypes = {"Created", "Updated", "False"},
        configClazz = TbCreateAlarmNodeConfiguration.class,
        nodeDescription = "Create or Update Alarm",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                        "Node output:\n" +
                        "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'metadata' will contains one of those properties 'isNewAlarm/isExistingAlarm'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateAlarmConfig",
        icon = "notifications_active"
)
public class TbCreateAlarmNode extends TbAbstractAlarmNode<TbCreateAlarmNodeConfiguration> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private List<String> relationTypes;
    private AlarmSeverity notDynamicAlarmSeverity;

    @Override
    public void init(@NotNull TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        if(!this.config.isDynamicSeverity()) {
            this.notDynamicAlarmSeverity = EnumUtils.getEnum(AlarmSeverity.class, this.config.getSeverity());
            if(this.notDynamicAlarmSeverity == null) {
                throw new TbNodeException("Incorrect Alarm Severity value: " + this.config.getSeverity());
            }
        }
    }


    @NotNull
    @Override
    protected TbCreateAlarmNodeConfiguration loadAlarmNodeConfig(@NotNull TbNodeConfiguration configuration) throws TbNodeException {
        TbCreateAlarmNodeConfiguration nodeConfiguration = TbNodeUtils.convert(configuration, TbCreateAlarmNodeConfiguration.class);
        relationTypes = nodeConfiguration.getRelationTypes();
        return nodeConfiguration;
    }

    @Nullable
    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        String alarmType;
        @Nullable final Alarm msgAlarm;

        if (!config.isUseMessageAlarmData()) {
            alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg);
            msgAlarm = null;
        } else {
            try {
                msgAlarm = getAlarmFromMessage(ctx, msg);
                alarmType = msgAlarm.getType();
            } catch (IOException e) {
                ctx.tellFailure(msg, e);
                return null;
            }
        }

        ListenableFuture<Alarm> latest = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        return Futures.transformAsync(latest, existingAlarm -> {
            if (existingAlarm == null || existingAlarm.getStatus().isCleared()) {
                return createNewAlarm(ctx, msg, msgAlarm);
            } else {
                return updateAlarm(ctx, msg, existingAlarm, msgAlarm);
            }
        }, ctx.getDbCallbackExecutor());

    }

    @NotNull
    private Alarm getAlarmFromMessage(@NotNull TbContext ctx, @NotNull TbMsg msg) throws IOException {
        Alarm msgAlarm;
        msgAlarm = mapper.readValue(msg.getData(), Alarm.class);
        msgAlarm.setTenantId(ctx.getTenantId());
        if (msgAlarm.getOriginator() == null) {
            msgAlarm.setOriginator(msg.getOriginator());
        }
        if (msgAlarm.getStatus() == null) {
            msgAlarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        }
        return msgAlarm;
    }

    @NotNull
    private ListenableFuture<TbAlarmResult> createNewAlarm(@NotNull TbContext ctx, @NotNull TbMsg msg, @Nullable Alarm msgAlarm) {
        ListenableFuture<JsonNode> asyncDetails;
        boolean buildDetails = !config.isUseMessageAlarmData() || config.isOverwriteAlarmDetails();
        if (buildDetails) {
            ctx.logJsEvalRequest();
            asyncDetails = buildAlarmDetails(ctx, msg, null);
        } else {
            asyncDetails = Futures.immediateFuture(null);
        }
        @NotNull ListenableFuture<Alarm> asyncAlarm =  Futures.transform(asyncDetails, details -> {
            if (buildDetails) {
                ctx.logJsEvalResponse();
            }
            Alarm newAlarm;
            if (msgAlarm != null) {
                newAlarm = msgAlarm;
                if (buildDetails) {
                    newAlarm.setDetails(details);
                }
            } else {
                newAlarm = buildAlarm(msg, details, ctx.getTenantId());
            }
            return newAlarm;
        }, MoreExecutors.directExecutor());
        @NotNull ListenableFuture<Alarm> asyncCreated = Futures.transform(asyncAlarm,
                alarm -> ctx.getAlarmService().createOrUpdateAlarm(alarm), ctx.getDbCallbackExecutor());
        return Futures.transform(asyncCreated, alarm -> new TbAlarmResult(true, false, false, alarm), MoreExecutors.directExecutor());
    }

    @NotNull
    private ListenableFuture<TbAlarmResult> updateAlarm(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull Alarm existingAlarm, @Nullable Alarm msgAlarm) {
        ListenableFuture<JsonNode> asyncDetails;
        boolean buildDetails = !config.isUseMessageAlarmData() || config.isOverwriteAlarmDetails();
        if (buildDetails) {
            ctx.logJsEvalRequest();
            asyncDetails = buildAlarmDetails(ctx, msg, existingAlarm.getDetails());
        } else {
            asyncDetails = Futures.immediateFuture(null);
        }
        @NotNull ListenableFuture<Alarm> asyncUpdated = Futures.transform(asyncDetails, details -> {
            if (buildDetails) {
                ctx.logJsEvalResponse();
            }
            if (msgAlarm != null) {
                existingAlarm.setSeverity(msgAlarm.getSeverity());
                existingAlarm.setPropagate(msgAlarm.isPropagate());
                existingAlarm.setPropagateToOwner(msgAlarm.isPropagateToOwner());
                existingAlarm.setPropagateToTenant(msgAlarm.isPropagateToTenant());
                existingAlarm.setPropagateRelationTypes(msgAlarm.getPropagateRelationTypes());
                if (buildDetails) {
                    existingAlarm.setDetails(details);
                } else {
                    existingAlarm.setDetails(msgAlarm.getDetails());
                }
            } else {
                existingAlarm.setSeverity(processAlarmSeverity(msg));
                existingAlarm.setPropagate(config.isPropagate());
                existingAlarm.setPropagateToOwner(config.isPropagateToOwner());
                existingAlarm.setPropagateToTenant(config.isPropagateToTenant());
                existingAlarm.setPropagateRelationTypes(relationTypes);
                existingAlarm.setDetails(details);
            }
            existingAlarm.setEndTs(System.currentTimeMillis());
            return ctx.getAlarmService().createOrUpdateAlarm(existingAlarm);
        }, ctx.getDbCallbackExecutor());

        return Futures.transform(asyncUpdated, a -> new TbAlarmResult(false, true, false, a), MoreExecutors.directExecutor());
    }

    private Alarm buildAlarm(@NotNull TbMsg msg, JsonNode details, TenantId tenantId) {
        long ts = msg.getMetaDataTs();
        return Alarm.builder()
                .tenantId(tenantId)
                .originator(msg.getOriginator())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(this.config.isDynamicSeverity() ? processAlarmSeverity(msg) : notDynamicAlarmSeverity)
                .propagate(config.isPropagate())
                .propagateToOwner(config.isPropagateToOwner())
                .propagateToTenant(config.isPropagateToTenant())
                .type(TbNodeUtils.processPattern(this.config.getAlarmType(), msg))
                .propagateRelationTypes(relationTypes)
                .startTs(ts)
                .endTs(ts)
                .details(details)
                .build();
    }

    @NotNull
    private AlarmSeverity processAlarmSeverity(@NotNull TbMsg msg) {
        AlarmSeverity severity = EnumUtils.getEnum(AlarmSeverity.class, TbNodeUtils.processPattern(this.config.getSeverity(), msg));
        if(severity == null) {
            throw new RuntimeException("Used incorrect pattern or Alarm Severity not included in message");
        }
        return severity;
    }

}
