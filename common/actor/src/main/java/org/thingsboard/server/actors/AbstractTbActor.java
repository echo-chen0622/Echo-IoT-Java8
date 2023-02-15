package org.thingsboard.server.actors;

import lombok.Getter;

public abstract class AbstractTbActor implements TbActor {

    @Getter
    protected TbActorCtx ctx;

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        this.ctx = ctx;
    }

    @Override
    public TbActorRef getActorRef() {
        return ctx;
    }
}
