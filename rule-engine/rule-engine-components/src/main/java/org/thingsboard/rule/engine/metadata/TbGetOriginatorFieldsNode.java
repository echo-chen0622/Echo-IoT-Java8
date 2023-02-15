package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesFieldsAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

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
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbGetOriginatorFieldsConfiguration.class);
        ignoreNullStrings = config.isIgnoreNullStrings();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            withCallback(putEntityFields(ctx, msg.getOriginator(), msg),
                    i -> ctx.tellSuccess(msg), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    private ListenableFuture<Void> putEntityFields(TbContext ctx, EntityId entityId, TbMsg msg) {
        if (config.getFieldsMapping().isEmpty()) {
            return Futures.immediateFuture(null);
        } else {
            return Futures.transform(EntitiesFieldsAsyncLoader.findAsync(ctx, entityId),
                    data -> {
                        config.getFieldsMapping().forEach((field, metaKey) -> {
                            String val = data.getFieldValue(field, ignoreNullStrings);
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
