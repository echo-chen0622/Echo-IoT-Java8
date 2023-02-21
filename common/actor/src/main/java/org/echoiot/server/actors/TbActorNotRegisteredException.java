package org.echoiot.server.actors;

import lombok.Getter;

public class TbActorNotRegisteredException extends RuntimeException {

    @Getter
    private final TbActorId target;

    public TbActorNotRegisteredException(TbActorId target, String message) {
        super(message);
        this.target = target;
    }
}
