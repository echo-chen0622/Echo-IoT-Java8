package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

public interface TbActorRef {

    TbActorId getActorId();

    void tell(TbActorMsg actorMsg);

    void tellWithHighPriority(TbActorMsg actorMsg);

}
