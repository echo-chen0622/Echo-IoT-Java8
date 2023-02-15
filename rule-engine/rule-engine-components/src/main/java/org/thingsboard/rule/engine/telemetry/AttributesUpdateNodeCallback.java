package org.thingsboard.rule.engine.telemetry;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.Nullable;
import java.util.List;

public class AttributesUpdateNodeCallback extends TelemetryNodeCallback {

    private final String scope;
    private final List<AttributeKvEntry> attributes;

    public AttributesUpdateNodeCallback(TbContext ctx, TbMsg msg, String scope, List<AttributeKvEntry> attributes) {
        super(ctx, msg);
        this.scope = scope;
        this.attributes = attributes;
    }

    @Override
    public void onSuccess(@Nullable Void result) {
        TbContext ctx = this.getCtx();
        TbMsg tbMsg = this.getMsg();
        ctx.enqueue(ctx.attributesUpdatedActionMsg(tbMsg.getOriginator(), ctx.getSelfId(), scope, attributes),
                () -> ctx.tellSuccess(tbMsg),
                throwable -> ctx.tellFailure(tbMsg, throwable));
    }
}
