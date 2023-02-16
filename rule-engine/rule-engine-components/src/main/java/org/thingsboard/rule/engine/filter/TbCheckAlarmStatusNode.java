package org.thingsboard.rule.engine.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;

import javax.annotation.Nullable;
import java.io.IOException;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check alarm status",
        configClazz = TbCheckAlarmStatusNodeConfig.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Checks alarm status.",
        nodeDetails = "Checks the alarm status to match one of the specified statuses.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckAlarmStatusConfig")
public class TbCheckAlarmStatusNode implements TbNode {
    private TbCheckAlarmStatusNodeConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckAlarmStatusNodeConfig.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            Alarm alarm = mapper.readValue(msg.getData(), Alarm.class);

            ListenableFuture<Alarm> latest = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), alarm.getId());

            Futures.addCallback(latest, new FutureCallback<Alarm>() {
                @Override
                public void onSuccess(@Nullable Alarm result) {
                    if (result != null) {
                        boolean isPresent = false;
                        for (AlarmStatus alarmStatus : config.getAlarmStatusList()) {
                            if (result.getStatus() == alarmStatus) {
                                isPresent = true;
                                break;
                            }
                        }
                        if (isPresent) {
                            ctx.tellNext(msg, "True");
                        } else {
                            ctx.tellNext(msg, "False");
                        }
                    } else {
                        ctx.tellFailure(msg, new TbNodeException("No such alarm found."));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    ctx.tellFailure(msg, t);
                }
            }, MoreExecutors.directExecutor());
        } catch (IOException e) {
            log.error("Failed to parse alarm: [{}]", msg.getData());
            throw new TbNodeException(e);
        }
    }

}
