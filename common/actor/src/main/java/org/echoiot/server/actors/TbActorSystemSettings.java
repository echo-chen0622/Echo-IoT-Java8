package org.echoiot.server.actors;

import lombok.Data;

@Data
public class TbActorSystemSettings {

    private final int actorThroughput;
    private final int schedulerPoolSize;
    private final int maxActorInitAttempts;

}
