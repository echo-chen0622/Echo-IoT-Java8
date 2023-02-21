package org.echoiot.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.server.common.msg.TbMsg;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
public class AttributesDeleteNodeCallback extends TelemetryNodeCallback {

    private final String scope;
    private final List<String> keys;

    public AttributesDeleteNodeCallback(TbContext ctx, TbMsg msg, String scope, List<String> keys) {
        super(ctx, msg);
        this.scope = scope;
        this.keys = keys;
    }

    @Override
    public void onSuccess(@Nullable Void result) {
        TbContext ctx = this.getCtx();
        TbMsg tbMsg = this.getMsg();
        ctx.enqueue(ctx.attributesDeletedActionMsg(tbMsg.getOriginator(), ctx.getSelfId(), scope, keys),
                () -> ctx.tellSuccess(tbMsg),
                throwable -> ctx.tellFailure(tbMsg, throwable));
    }
}
