package org.echoiot.rule.engine.transform;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.echoiot.common.util.ListeningExecutor;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.dao.asset.AssetService;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.echoiot.rule.engine.api.TbRelationTypes.FAILURE;

@RunWith(MockitoJUnitRunner.class)
public class TbChangeOriginatorNodeTest {

    private TbChangeOriginatorNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private AssetService assetService;

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
    public void originatorCanBeChangedToCustomerId() throws TbNodeException {
        init();
        @NotNull AssetId assetId = new AssetId(Uuids.timeBased());
        @NotNull CustomerId customerId = new CustomerId(Uuids.timeBased());
        @NotNull Asset asset = new Asset();
        asset.setCustomerId(customerId);

        @NotNull RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        @NotNull RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        @NotNull TbMsg msg = TbMsg.newMsg("ASSET", assetId, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(),eq( assetId))).thenReturn(Futures.immediateFuture(asset));

        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(customerId, originatorCaptor.getValue());
    }

    @Test
    public void newChainCanBeStarted() throws TbNodeException {
        init();
        @NotNull AssetId assetId = new AssetId(Uuids.timeBased());
        @NotNull CustomerId customerId = new CustomerId(Uuids.timeBased());
        @NotNull Asset asset = new Asset();
        asset.setCustomerId(customerId);

        @NotNull RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        @NotNull RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        @NotNull TbMsg msg = TbMsg.newMsg("ASSET", assetId, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        node.onMsg(ctx, msg);
        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(customerId, originatorCaptor.getValue());
    }

    @Test
    public void exceptionThrownIfCannotFindNewOriginator() throws TbNodeException {
        init();
        @NotNull AssetId assetId = new AssetId(Uuids.timeBased());
        @NotNull CustomerId customerId = new CustomerId(Uuids.timeBased());
        @NotNull Asset asset = new Asset();
        asset.setCustomerId(customerId);

        @NotNull RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        @NotNull RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        @NotNull TbMsg msg = TbMsg.newMsg("ASSET", assetId, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(same(msg), same(FAILURE));
    }

    public void init() throws TbNodeException {
        @NotNull TbChangeOriginatorNodeConfiguration config = new TbChangeOriginatorNodeConfiguration();
        config.setOriginatorSource(TbChangeOriginatorNode.CUSTOMER_SOURCE);
        @NotNull ObjectMapper mapper = new ObjectMapper();
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbChangeOriginatorNode();
        node.init(null, nodeConfiguration);
    }
}
