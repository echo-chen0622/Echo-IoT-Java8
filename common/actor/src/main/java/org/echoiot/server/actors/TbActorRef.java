package org.echoiot.server.actors;

import org.echoiot.server.common.msg.TbActorMsg;

public interface TbActorRef {

    TbActorId getActorId();

    void tell(TbActorMsg actorMsg);

    void tellWithHighPriority(TbActorMsg actorMsg);

}
