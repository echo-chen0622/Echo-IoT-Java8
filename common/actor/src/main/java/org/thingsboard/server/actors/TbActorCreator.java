package org.thingsboard.server.actors;

public interface TbActorCreator {

    TbActorId createActorId();

    TbActor createActor();

}
