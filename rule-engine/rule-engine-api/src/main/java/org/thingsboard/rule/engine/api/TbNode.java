package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.concurrent.ExecutionException;

/**
 * Created by Echo on 19.01.18.
 */
public interface TbNode {

    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    default void destroy() {}

    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {}

}
