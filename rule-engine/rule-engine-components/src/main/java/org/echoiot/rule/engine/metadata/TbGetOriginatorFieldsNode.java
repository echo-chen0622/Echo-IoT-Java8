package org.echoiot.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.rule.engine.util.EntitiesFieldsAsyncLoader;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.echoiot.common.util.DonAsynchron.withCallback;

/**
 * Created by Echo on 19.01.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "originator fields",
        configClazz = TbGetOriginatorFieldsConfiguration.class,
        nodeDescription = "Add Message Originator fields values into Message Metadata",
        nodeDetails = "Will fetch fields values specified in mapping. If specified field is not part of originator fields it will be ignored.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeOriginatorFieldsConfig")
public class TbGetOriginatorFieldsNode implements TbNode {

    private TbGetOriginatorFieldsConfiguration config;
    private boolean ignoreNullStrings;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbGetOriginatorFieldsConfiguration.class);
        ignoreNullStrings = config.isIgnoreNullStrings();
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        try {
            withCallback(putEntityFields(ctx, msg.getOriginator(), msg),
                    i -> ctx.tellSuccess(msg), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    @NotNull
    private ListenableFuture<Void> putEntityFields(@NotNull TbContext ctx, @NotNull EntityId entityId, @NotNull TbMsg msg) {
        if (config.getFieldsMapping().isEmpty()) {
            return Futures.immediateFuture(null);
        } else {
            return Futures.transform(EntitiesFieldsAsyncLoader.findAsync(ctx, entityId),
                    data -> {
                        config.getFieldsMapping().forEach((field, metaKey) -> {
                            @Nullable String val = data.getFieldValue(field, ignoreNullStrings);
                            if (val != null) {
                                msg.getMetaData().putValue(metaKey, val);
                            }
                        });
                        return null;
                    }, MoreExecutors.directExecutor()
            );
        }
    }

}
