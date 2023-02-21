package org.echoiot.rule.engine.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.echoiot.common.util.CollectionsUtil;
import org.echoiot.common.util.DonAsynchron;
import org.echoiot.rule.engine.api.EmptyNodeConfiguration;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.objects.AttributesEntityView;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.common.transport.adaptor.JsonConverter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.echoiot.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "copy to view",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Copy attributes from asset/device to entity view and changes message originator to related entity view",
        nodeDetails = "Copy attributes from asset/device to related entity view according to entity view configuration. \n " +
                "Copy will be done only for attributes that are between start and end dates and according to attribute keys configuration. \n" +
                "Changes message originator to related entity view and produces new messages according to count of updated entity views",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig",
        icon = "content_copy"
)
public class TbCopyAttributesToEntityViewNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        if (DataConstants.ATTRIBUTES_UPDATED.equals(msg.getType()) ||
                DataConstants.ATTRIBUTES_DELETED.equals(msg.getType()) ||
                DataConstants.ACTIVITY_EVENT.equals(msg.getType()) ||
                DataConstants.INACTIVITY_EVENT.equals(msg.getType()) ||
                SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msg.getType())) {
            if (!msg.getMetaData().getData().isEmpty()) {
                long now = System.currentTimeMillis();
                String scope = msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) ?
                        DataConstants.CLIENT_SCOPE : msg.getMetaData().getValue(DataConstants.SCOPE);

                ListenableFuture<List<EntityView>> entityViewsFuture =
                        ctx.getEntityViewService().findEntityViewsByTenantIdAndEntityIdAsync(ctx.getTenantId(), msg.getOriginator());

                DonAsynchron.withCallback(entityViewsFuture,
                        entityViews -> {
                            for (@NotNull EntityView entityView : entityViews) {
                                long startTime = entityView.getStartTimeMs();
                                long endTime = entityView.getEndTimeMs();
                                if ((endTime != 0 && endTime > now && startTime < now) || (endTime == 0 && startTime < now)) {
                                    if (DataConstants.ATTRIBUTES_DELETED.equals(msg.getType())) {
                                        @NotNull List<String> attributes = new ArrayList<>();
                                        for (@NotNull JsonElement element : new JsonParser().parse(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray()) {
                                            if (element.isJsonPrimitive()) {
                                                JsonPrimitive value = element.getAsJsonPrimitive();
                                                if (value.isString()) {
                                                    attributes.add(value.getAsString());
                                                }
                                            }
                                        }
                                        @NotNull List<String> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr, entityView)).collect(Collectors.toList());
                                        if (!filteredAttributes.isEmpty()) {
                                            ctx.getTelemetryService().deleteAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                    getFutureCallback(ctx, msg, entityView));
                                        }
                                    } else {
                                        @NotNull Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
                                        @NotNull List<AttributeKvEntry> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr.getKey(), entityView)).collect(Collectors.toList());
                                        ctx.getTelemetryService().saveAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                getFutureCallback(ctx, msg, entityView));
                                    }
                                }
                            }
                            ctx.ack(msg);
                        },
                        t -> ctx.tellFailure(msg, t));
            } else {
                ctx.tellFailure(msg, new IllegalArgumentException("Message metadata is empty"));
            }
        } else {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type [" + msg.getType() + "]"));
        }
    }

    @NotNull
    private FutureCallback<Void> getFutureCallback(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull EntityView entityView) {
        return new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                transformAndTellNext(ctx, msg, entityView);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    private void transformAndTellNext(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull EntityView entityView) {
        ctx.enqueueForTellNext(ctx.newMsg(msg.getQueueName(), msg.getType(), entityView.getId(), msg.getCustomerId(), msg.getMetaData(), msg.getData()), SUCCESS);
    }

    private boolean attributeContainsInEntityView(@NotNull String scope, String attrKey, @NotNull EntityView entityView) {
        AttributesEntityView attributesEntityView = entityView.getKeys().getAttributes();
        @org.jetbrains.annotations.Nullable List<String> keys = null;
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                keys = attributesEntityView.getCs();
                break;
            case DataConstants.SERVER_SCOPE:
                keys = attributesEntityView.getSs();
                break;
            case DataConstants.SHARED_SCOPE:
                keys = attributesEntityView.getSh();
                break;
        }
        return CollectionsUtil.contains(keys, attrKey);
    }

}
