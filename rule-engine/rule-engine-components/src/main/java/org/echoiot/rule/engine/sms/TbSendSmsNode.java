package org.echoiot.rule.engine.sms;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.sms.SmsSender;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;

import static org.echoiot.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send sms",
        configClazz = TbSendSmsNodeConfiguration.class,
        nodeDescription = "Sends SMS message via SMS provider.",
        nodeDetails = "Will send SMS message by populating target phone numbers and sms message fields using values derived from message metadata.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeSendSmsConfig",
        icon = "sms"
)
public class TbSendSmsNode implements TbNode {

    private TbSendSmsNodeConfiguration config;
    private SmsSender smsSender;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        try {
            this.config = TbNodeUtils.convert(configuration, TbSendSmsNodeConfiguration.class);
            if (!this.config.isUseSystemSmsSettings()) {
                smsSender = createSmsSender(ctx);
            }
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            withCallback(ctx.getSmsExecutor().executeAsync(() -> {
                        sendSms(ctx, msg);
                        return null;
                    }),
                    ok -> ctx.tellSuccess(msg),
                    fail -> ctx.tellFailure(msg, fail));
        } catch (Exception ex) {
            ctx.tellFailure(msg, ex);
        }
    }

    private void sendSms(TbContext ctx, TbMsg msg) throws Exception {
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersToTemplate(), msg);
        String message = TbNodeUtils.processPattern(this.config.getSmsMessageTemplate(), msg);
        String[] numbersToList = numbersTo.split(",");
        if (this.config.isUseSystemSmsSettings()) {
            ctx.getSmsService().sendSms(ctx.getTenantId(), msg.getCustomerId(), numbersToList, message);
        } else {
            for (String numberTo : numbersToList) {
                this.smsSender.sendSms(numberTo, message);
            }
        }
    }

    @Override
    public void destroy() {
        if (this.smsSender != null) {
            this.smsSender.destroy();
        }
    }

    private SmsSender createSmsSender(TbContext ctx) {
        return ctx.getSmsSenderFactory().createSmsSender(this.config.getSmsProviderConfiguration());
    }

}