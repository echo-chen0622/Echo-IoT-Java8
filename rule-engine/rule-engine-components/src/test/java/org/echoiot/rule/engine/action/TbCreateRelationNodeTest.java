package org.echoiot.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.common.util.ListeningExecutor;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.relation.RelationService;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbCreateRelationNodeTest {

    private static final String RELATION_TYPE_CONTAINS = "Contains";

    private TbCreateRelationNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private AssetService assetService;
    @Mock
    private RelationService relationService;

    private TbMsg msg;

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private ListeningExecutor dbExecutor;

    @Before
    public void before() {
        dbExecutor = new ListeningExecutor() {
            @NotNull
            @Override
            public <T> ListenableFuture<T> executeAsync(@NotNull Callable<T> task) {
                try {
                    return Futures.immediateFuture(task.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void execute(@NotNull Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void testCreateNewRelation() throws TbNodeException {
        init(createRelationNodeConfig());

        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());

        @NotNull AssetId assetId = new AssetId(Uuids.timeBased());
        @NotNull Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbRelationTypes.SUCCESS);
    }

    @Test
    public void testDeleteCurrentRelationsCreateNewRelation() throws TbNodeException {
        init(createRelationNodeConfigWithRemoveCurrentRelations());

        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());

        @NotNull AssetId assetId = new AssetId(Uuids.timeBased());
        @NotNull Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        @NotNull EntityRelation relation = new EntityRelation();
        when(ctx.getRelationService().findByToAndTypeAsync(any(), eq(msg.getOriginator()), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(Collections.singletonList(relation)));
        when(ctx.getRelationService().deleteRelationAsync(any(), eq(relation))).thenReturn(Futures.immediateFuture(true));
        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbRelationTypes.SUCCESS);
    }

    @Test
    public void testCreateNewRelationAndChangeOriginator() throws TbNodeException {
        init(createRelationNodeConfigWithChangeOriginator());

        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());

        @NotNull AssetId assetId = new AssetId(Uuids.timeBased());
        @NotNull Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(assetId, originatorCaptor.getValue());
    }

    public void init(TbCreateRelationNodeConfiguration configuration) throws TbNodeException {
        @NotNull ObjectMapper mapper = new ObjectMapper();
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(configuration));

        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getAssetService()).thenReturn(assetService);

        node = new TbCreateRelationNode();
        node.init(ctx, nodeConfiguration);
    }

    @NotNull
    private TbCreateRelationNodeConfiguration createRelationNodeConfig() {
        @NotNull TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType(RELATION_TYPE_CONTAINS);
        configuration.setEntityCacheExpiration(300);
        configuration.setEntityType("ASSET");
        configuration.setEntityNamePattern("${name}");
        configuration.setEntityTypePattern("${type}");
        configuration.setCreateEntityIfNotExists(false);
        configuration.setChangeOriginatorToRelatedEntity(false);
        configuration.setRemoveCurrentRelations(false);
        return configuration;
    }

    @NotNull
    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithRemoveCurrentRelations() {
        @NotNull TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setRemoveCurrentRelations(true);
        return configuration;
    }

    @NotNull
    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithChangeOriginator() {
        @NotNull TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setChangeOriginatorToRelatedEntity(true);
        return configuration;
    }
}
