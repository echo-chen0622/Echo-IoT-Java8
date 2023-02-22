package org.echoiot.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.msg.TbMsg;

@Slf4j
public abstract class TbAbstractTypeSwitchNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ctx.tellNext(msg, getRelationType(ctx, msg.getOriginator()));
    }

    protected abstract String getRelationType(TbContext ctx, EntityId originator) throws TbNodeException;

}
