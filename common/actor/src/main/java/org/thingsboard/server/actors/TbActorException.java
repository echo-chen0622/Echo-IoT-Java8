package org.thingsboard.server.actors;

public class TbActorException extends Exception {

    private static final long serialVersionUID = 8209771144711980882L;

    public TbActorException(String message, Throwable cause) {
        super(message, cause);
    }
}
