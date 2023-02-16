package org.echoiot.server.actors;

public interface TbActorCreator {

    TbActorId createActorId();

    TbActor createActor();

}
