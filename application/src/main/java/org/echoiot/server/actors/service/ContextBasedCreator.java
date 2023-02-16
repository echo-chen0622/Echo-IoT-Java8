package org.echoiot.server.actors.service;

import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.actors.TbActorCreator;

public abstract class ContextBasedCreator implements TbActorCreator {

    protected final transient ActorSystemContext context;

    public ContextBasedCreator(ActorSystemContext context) {
        super();
        this.context = context;
    }
}
