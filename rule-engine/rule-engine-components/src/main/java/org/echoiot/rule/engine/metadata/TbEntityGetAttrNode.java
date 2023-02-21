package org.echoiot.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.KvEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.echoiot.common.util.DonAsynchron.withCallback;
import static org.echoiot.rule.engine.api.TbRelationTypes.FAILURE;
import static org.echoiot.server.common.data.DataConstants.SERVER_SCOPE;

@Slf4j
public abstract class TbEntityGetAttrNode<T extends EntityId> implements TbNode {

    private TbGetEntityAttrNodeConfiguration config;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetEntityAttrNodeConfiguration.class);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        try {
            withCallback(findEntityAsync(ctx, msg.getOriginator()),
                    entityId -> safeGetAttributes(ctx, msg, entityId),
                    t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    private void safeGetAttributes(@NotNull TbContext ctx, @NotNull TbMsg msg, @Nullable T entityId) {
        if (entityId == null || entityId.isNullUid()) {
            ctx.tellNext(msg, FAILURE);
            return;
        }

        @NotNull Map<String, String> mappingsMap = new HashMap<>();
        config.getAttrMapping().forEach((key, value) -> {
            String processPatternKey = TbNodeUtils.processPattern(key, msg);
            String processPatternValue = TbNodeUtils.processPattern(value, msg);
            mappingsMap.put(processPatternKey, processPatternValue);
        });

        List<String> keys = List.copyOf(mappingsMap.keySet());
        withCallback(config.isTelemetry() ? getLatestTelemetry(ctx, entityId, keys) : getAttributesAsync(ctx, entityId, keys),
                attributes -> putAttributesAndTell(ctx, msg, attributes, mappingsMap),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<List<KvEntry>> getAttributesAsync(@NotNull TbContext ctx, EntityId entityId, List<String> attrKeys) {
        ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, SERVER_SCOPE, attrKeys);
        return Futures.transform(latest, l ->
                l.stream().map(i -> (KvEntry) i).collect(Collectors.toList()), MoreExecutors.directExecutor());
    }

    @NotNull
    private ListenableFuture<List<KvEntry>> getLatestTelemetry(@NotNull TbContext ctx, EntityId entityId, List<String> timeseriesKeys) {
        ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, timeseriesKeys);
        return Futures.transform(latest, l ->
                l.stream().map(i -> (KvEntry) i).collect(Collectors.toList()), MoreExecutors.directExecutor());
    }


    private void putAttributesAndTell(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull List<? extends KvEntry> attributes, @NotNull Map<String, String> map) {
        attributes.forEach(r -> {
            String attrName = map.get(r.getKey());
            msg.getMetaData().putValue(attrName, r.getValueAsString());
        });
        ctx.tellSuccess(msg);
    }

    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

    public void setConfig(TbGetEntityAttrNodeConfiguration config) {
        this.config = config;
    }

}
