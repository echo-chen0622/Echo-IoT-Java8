package org.thingsboard.server.common.msg;

/**
 * Created by ashvayka on 15.03.18.
 */
public interface TbActorMsg {

    MsgType getMsgType();

    /**
     * Executed when the target TbActor is stopped or destroyed.
     * For example, rule node failed to initialize or removed from rule chain.
     * Implementation should cleanup the resources.
     */
    default void onTbActorStopped(TbActorStopReason reason) {
    }

}
