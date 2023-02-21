package org.echoiot.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.echoiot.common.util.DonAsynchron.withCallback;
import static org.echoiot.rule.engine.api.TbRelationTypes.FAILURE;

/**
 * Created by Echo on 19.01.18.
 */
@Slf4j
public abstract class TbAbstractTransformNode implements TbNode {

    private TbTransformNodeConfiguration config;

    @Override
    public void init(TbContext context, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTransformNodeConfiguration.class);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, TbMsg msg) {
        withCallback(transform(ctx, msg),
                m -> transformSuccess(ctx, msg, m),
                t -> transformFailure(ctx, msg, t),
                MoreExecutors.directExecutor());
    }

    protected void transformFailure(@NotNull TbContext ctx, TbMsg msg, Throwable t) {
        ctx.tellFailure(msg, t);
    }

    protected void transformSuccess(@NotNull TbContext ctx, TbMsg msg, @Nullable TbMsg m) {
        if (m != null) {
            ctx.tellSuccess(m);
        } else {
            ctx.tellNext(msg, FAILURE);
        }
    }

    protected void transformSuccess(@NotNull TbContext ctx, TbMsg msg, @Nullable List<TbMsg> msgs) {
        if (msgs != null && !msgs.isEmpty()) {
            if (msgs.size() == 1) {
                ctx.tellSuccess(msgs.get(0));
            } else {
                @NotNull TbMsgCallbackWrapper wrapper = new MultipleTbMsgsCallbackWrapper(msgs.size(), new TbMsgCallback() {
                    @Override
                    public void onSuccess() {
                        ctx.ack(msg);
                    }

                    @Override
                    public void onFailure(RuleEngineException e) {
                        ctx.tellFailure(msg, e);
                    }
                });
                msgs.forEach(newMsg -> ctx.enqueueForTellNext(newMsg, TbRelationTypes.SUCCESS, wrapper::onSuccess, wrapper::onFailure));
            }
        } else {
            ctx.tellNext(msg, FAILURE);
        }
    }

    protected abstract ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg);

    public void setConfig(TbTransformNodeConfiguration config) {
        this.config = config;
    }
}
