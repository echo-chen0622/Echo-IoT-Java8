package org.echoiot.server.common.msg;

import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.gen.MsgProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Echo on 13.01.18.
 */
public final class TbMsgProcessingCtx implements Serializable {

    @NotNull
    private final AtomicInteger ruleNodeExecCounter;
    private volatile LinkedList<TbMsgProcessingStackItem> stack;

    public TbMsgProcessingCtx() {
        this(0);
    }

    public TbMsgProcessingCtx(int ruleNodeExecCounter) {
        this(ruleNodeExecCounter, null);
    }

    private TbMsgProcessingCtx(int ruleNodeExecCounter, LinkedList<TbMsgProcessingStackItem> stack) {
        this.ruleNodeExecCounter = new AtomicInteger(ruleNodeExecCounter);
        this.stack = stack;
    }

    public int getAndIncrementRuleNodeCounter() {
        return ruleNodeExecCounter.getAndIncrement();
    }

    @NotNull
    public TbMsgProcessingCtx copy() {
        if (stack == null || stack.isEmpty()) {
            return new TbMsgProcessingCtx(ruleNodeExecCounter.get());
        } else {
            return new TbMsgProcessingCtx(ruleNodeExecCounter.get(), new LinkedList<>(stack));
        }
    }

    public void push(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        if (stack == null) {
            stack = new LinkedList<>();
        }
        stack.add(new TbMsgProcessingStackItem(ruleChainId, ruleNodeId));
    }

    @Nullable
    public TbMsgProcessingStackItem pop() {
        return !stack.isEmpty() ? stack.removeLast() : null;
    }

    @NotNull
    public static TbMsgProcessingCtx fromProto(@NotNull MsgProtos.TbMsgProcessingCtxProto ctx) {
        int ruleNodeExecCounter = ctx.getRuleNodeExecCounter();
        if (ctx.getStackCount() > 0) {
            @NotNull LinkedList<TbMsgProcessingStackItem> stack = new LinkedList<>();
            for (@NotNull MsgProtos.TbMsgProcessingStackItemProto item : ctx.getStackList()) {
                stack.add(TbMsgProcessingStackItem.fromProto(item));
            }
            return new TbMsgProcessingCtx(ruleNodeExecCounter, stack);
        } else {
            return new TbMsgProcessingCtx(ruleNodeExecCounter);
        }
    }

    @NotNull
    public MsgProtos.TbMsgProcessingCtxProto toProto() {
        var ctxBuilder = MsgProtos.TbMsgProcessingCtxProto.newBuilder();
        ctxBuilder.setRuleNodeExecCounter(ruleNodeExecCounter.get());
        if (stack != null) {
            for (@NotNull TbMsgProcessingStackItem item : stack) {
                ctxBuilder.addStack(item.toProto());
            }
        }
        return ctxBuilder.build();
    }
}
