package org.echoiot.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "clear alarm", relationTypes = {"Cleared", "False"},
        configClazz = TbClearAlarmNodeConfiguration.class,
        nodeDescription = "Clear Alarm",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                        "Node output:\n" +
                        "If alarm was not cleared, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'metadata' will contains 'isClearedAlarm' property. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeClearAlarmConfig",
        icon = "notifications_off"
)
public class TbClearAlarmNode extends TbAbstractAlarmNode<TbClearAlarmNodeConfiguration> {

    @Override
    protected TbClearAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbClearAlarmNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        String alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg);
        ListenableFuture<Alarm> alarmFuture;
        if (msg.getOriginator().getEntityType().equals(EntityType.ALARM)) {
            alarmFuture = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), new AlarmId(msg.getOriginator().getId()));
        } else {
            alarmFuture = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        }
        return Futures.transformAsync(alarmFuture, a -> {
            if (a != null && !a.getStatus().isCleared()) {
                return clearAlarm(ctx, msg, a);
            }
            return Futures.immediateFuture(new TbAlarmResult(false, false, false, null));
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<TbAlarmResult> clearAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ctx.logJsEvalRequest();
        ListenableFuture<JsonNode> asyncDetails = buildAlarmDetails(ctx, msg, alarm.getDetails());
        return Futures.transformAsync(asyncDetails, details -> {
            ctx.logJsEvalResponse();
            ListenableFuture<Boolean> clearFuture = ctx.getAlarmService().clearAlarm(ctx.getTenantId(), alarm.getId(), details, System.currentTimeMillis());
            return Futures.transformAsync(clearFuture, cleared -> {
                ListenableFuture<Alarm> savedAlarmFuture = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), alarm.getId());
                return Futures.transformAsync(savedAlarmFuture, savedAlarm -> {
                    if (cleared && savedAlarm != null) {
                        alarm.setDetails(savedAlarm.getDetails());
                        alarm.setEndTs(savedAlarm.getEndTs());
                        alarm.setClearTs(savedAlarm.getClearTs());
                    }
                    alarm.setStatus(alarm.getStatus().isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK);
                    return Futures.immediateFuture(new TbAlarmResult(false, false, true, alarm));
                }, ctx.getDbCallbackExecutor());
            }, ctx.getDbCallbackExecutor());
        }, ctx.getDbCallbackExecutor());
    }
}