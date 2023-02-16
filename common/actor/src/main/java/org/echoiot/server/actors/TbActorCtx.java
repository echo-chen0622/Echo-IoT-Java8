package org.echoiot.server.actors;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.msg.TbActorMsg;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface TbActorCtx extends TbActorRef {

    TbActorId getSelf();

    TbActorRef getParentRef();

    void tell(TbActorId target, TbActorMsg msg);

    void stop(TbActorId target);

    TbActorRef getOrCreateChildActor(TbActorId actorId, Supplier<String> dispatcher, Supplier<TbActorCreator> creator);

    void broadcastToChildren(TbActorMsg msg);

    void broadcastToChildrenByType(TbActorMsg msg, EntityType entityType);

    void broadcastToChildren(TbActorMsg msg, Predicate<TbActorId> childFilter);

    List<TbActorId> filterChildren(Predicate<TbActorId> childFilter);
}
