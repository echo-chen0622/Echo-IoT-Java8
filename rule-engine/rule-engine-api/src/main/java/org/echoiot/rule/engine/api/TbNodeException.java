package org.echoiot.rule.engine.api;

/**
 * Created by ashvayka on 19.01.18.
 */
public class TbNodeException extends Exception {

    public TbNodeException(String message) {
        super(message);
    }

    public TbNodeException(Exception e) {
        super(e);
    }

}
