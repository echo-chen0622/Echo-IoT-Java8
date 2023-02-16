package org.echoiot.rule.engine.api;

import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.queue.PartitionChangeMsg;

import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 19.01.18.
 */
public interface TbNode {

    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    default void destroy() {}

    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {}

}